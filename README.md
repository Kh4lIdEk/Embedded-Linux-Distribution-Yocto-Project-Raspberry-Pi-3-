# Custom Embedded Linux Distribution & BME680 Web Telemetry (Yocto Project / Raspberry Pi 3)

> **Embedded Linux Engineering Project — Mines Saint-Étienne (ISMIN 3A)**
>
> **Authors:** Khalid Elkoussami
>
> **Academic Year:** 2026–2027

## Overview

This repository contains our custom **OpenEmbedded / Yocto Project** layer (`meta-bootlinlabs`), reference BitBake configurations (`conf-samples/`), and technical documentation (`docs/`) to build a tailored Embedded Linux distribution (`core-image-minimal`) for the **Raspberry Pi 3 Model B**.

Key features of our custom distribution include:

1. **Hybrid Boot Architecture**: Kernel and bootloader on SD card with a live root filesystem (`rootfs`) mounted over Ethernet via **NFSv3**.
2. **Custom Software Layer (`meta-bootlinlabs`)**: Cross-compiled applications (`ninvaders`, `ctris`), patched third-party utilities (`nano`), and custom kernel configuration fragments (`CONFIG_MODULE_COMPRESS_NONE=y`).
3. **Hardware** $\text{I}^2\text{C}$ **& Linux IIO Integration**: Kernel-level interfacing with a **Bosch BME680** environmental sensor (`0x76`) through the Linux **Industrial I/O (IIO)** subsystem (`sysfs`), initialized automatically at boot via an LSB-compliant **SysVinit** service.
4. **Embedded Web Telemetry Server**: A lightweight **Lighttpd + CGI** web stack serving a real-time dashboard that polls and formats temperature (C) and atmospheric pressure (hPa) every 5s.

## System Architecture

### 1. Hardware & Network Development Topology

The development environment runs on a **Debian 13** virtual machine (`dev@debian13`) hosted on a Windows PC. Two physical links connect the host to the **Raspberry Pi 3**:

* **USB-UART Serial Link (`/dev/ttyUSB0`** $\leftrightarrow$ **`/dev/ttyS0` at `115200` baud)**: Provides low-level bootloader and kernel console access via `picocom`.
* **Ethernet Link (`10.42.0.1/24`)**: Managed via NetworkManager connection sharing (`PARTAGE-RPi`), providing DHCP IP assignment (`10.42.0.159`), **Dropbear SSH** access, and an exported **NFSv3 root filesystem** (`/nfs`) so package updates take effect immediately without reflashing the SD card.

```
+-------------------------------------------------------+         +-----------------------------------------+
| Host PC (Debian 13 VM)                                |         | Target: Raspberry Pi 3 Model B          |
|                                                       |         |                                         |
|  [BitBake / Poky Scarthgap]                           |         |  [Bootloader + Kernel 6.6.63 (SD Card)] |
|         |                                             |         |         |                               |
|         v                                             |  UART   |         v                               |
|  [Rootfs Archive (.tar.bz2)] ===> [/nfs Export] <-----+---------+---> [/dev/ttyS0 Console (115200)]       |
|                                        |              |         |                                         |
|  [DHCP / NAT (10.42.0.1/24)] <---------+--------------+---------+---> [eth0 (10.42.0.159) - NFS Rootfs]   |
|  [Web Browser / SSH Client]  <---------+--------------+---------+---> [Dropbear SSH (:22) / Lighttpd (:80)]|
+-------------------------------------------------------+   ETH   +--------------------+--------------------+
                                                                                       |
                                                                            I2C1 Bus (GPIO2 / GPIO3)
                                                                                       |
                                                                                       v
                                                                          +-------------------------+
                                                                          | Bosch BME680 Sensor     |
                                                                          | Address: 0x76           |
                                                                          | (Temp, Pressure, Humid) |
                                                                          +-------------------------+
```

### 2. End-to-End Web Telemetry Data Flow

From hardware acquisition to browser rendering, the telemetry stack operates across three distinct layers without requiring heavy runtime frameworks:

```
[Client Browser (PC)]
        |  HTTP GET /cgi-bin/data.cgi (Every 5s via Fetch API)
        v
+-------------------------------------------------------------------+
| Userspace (Raspberry Pi 3)                                        |
|  [Lighttpd Server (Port 80, user: daemon)]                        |
|        |  mod_cgi (ENV)                                           |
|        v                                                          |
|  [/www/pages/cgi-bin/data.cgi (POSIX /bin/sh + BusyBox awk)]      |
+--------|----------------------------------------------------------+
         |  libC / VFS read()
+--------v----------------------------------------------------------+
| Linux Kernel (6.6.63)                                             |
|  [sysfs: /sys/bus/iio/devices/iio:device0/in_*_input]             |
|        ^                                                          |
|  [IIO Subsystem (industrialio + bme680_core + bme680_i2c)]        |
|        ^                                                          |
|  [I2C Subsystem (i2c-bcm2835 / i2c-dev)]                          |
+--------|----------------------------------------------------------+
         |  Physical I2C1 Bus (SDA / SCL)
+--------v----------------------------------------------------------+
| Hardware: Bosch BME680 Environmental Sensor (0x76)                |
+-------------------------------------------------------------------+
```

## Repository Structure

```
.
├── .gitignore
├── README.md
├── conf-samples/                           # Reference BitBake configuration files
│   ├── bblayers.conf.sample                # Active layers configuration
│   └── local.conf.sample                   # Machine, NFS cmdline, I2C, and package settings
├── docs/                                   # Complete lab report & architecture diagrams
│   └── Rapport_Yocto_BENJOUAD_ELKOUSSAMI.pdf
└── meta-bootlinlabs/                       # Custom Yocto Layer (Priority 7, Scarthgap)
    ├── COPYING.MIT
    ├── README
    ├── conf/
    │   └── layer.conf
    ├── recipes-games/
    │   ├── ctris/
    │   │   └── ctris_git.bb                # Terminal Tetris clone recipe
    │   └── ninvaders/
    │       ├── ninvaders.inc               # Shared build/install rules for nInvaders
    │       └── ninvaders_0.1.1.bb          # Checksum & license recipe
    ├── recipes-kernel/
    │   └── linux/
    │       ├── linux-raspberrypi/
    │       │   └── uncompressed_modules.cfg # Disables .ko.xz module compression
    │       └── linux-raspberrypi_%.bbappend # Appends config fragment to RPi kernel
    └── recipes-support/
        ├── bme680-init/                    # SysVinit script for BME680 IIO instantiation
        │   └── files/
        │       └── bme680
        └── web-telemetry/                  # Lighttpd configuration, CGI script & HTML UI
            └── files/
                ├── data.cgi
                ├── index.html
                └── lighttpd.conf
```

## Hardware Wiring Guide

### 1. Bosch BME680 Sensor ($\text{I}^2\text{C}$ Bus 1)

| BME680 Pin | Raspberry Pi 3 Pin | BCM GPIO | Function |
| ----- | ----- | ----- | ----- |
| **VCC / VIN** | Pin 1 | `3V3` | $3.3\text{ V}$ Power Supply |
| **GND** | Pin 6 | `GND` | Common Ground |
| **SDA** | Pin 3 | `GPIO 2 (SDA1)` | $\text{I}^2\text{C}$ Data Line |
| **SCL** | Pin 5 | `GPIO 3 (SCL1)` | $\text{I}^2\text{C}$ Clock Line |

### 2. USB-to-TTL Serial Adapter (UART0)

| USB-TTL Pin | Raspberry Pi 3 Pin | BCM GPIO | Function |
| ----- | ----- | ----- | ----- |
| **GND** | Pin 6 or 9 | `GND` | Common Ground |
| **RXD** | Pin 8 | `GPIO 14 (TXD0)` | Target UART Transmit |
| **TXD** | Pin 10 | `GPIO 15 (RXD0)` | Target UART Receive |

## How to Reproduce Our Distribution

### 1. Clone Layers & Initialize Build Environment
```bash
mkdir -p ~/Yocto-RPi && cd ~/Yocto-RPi
git clone -b scarthgap git://git.yoctoproject.org/poky
git clone -b scarthgap git://git.yoctoproject.org/meta-raspberrypi
git clone -b scarthgap https://git.openembedded.org/meta-openembedded poky/meta-openembedded
git clone https://github.com/Kh4lIdEk/Embedded-Linux-Distribution-Yocto-Project-Raspberry-Pi-3-.git repo-source
cp -r repo-source/meta-bootlinlabs ~/Yocto-RPi/

source poky/oe-init-build-env builds/rpi3
bitbake-layers add-layer ~/Yocto-RPi/meta-raspberrypi ~/Yocto-RPi/poky/meta-openembedded/meta-oe ~/Yocto-RPi/meta-bootlinlabs
```

### 2. Configure & Build Image
```bash
cp ~/Yocto-RPi/repo-source/conf-samples/local.conf.sample conf/local.conf
bitbake core-image-minimal
```

### 3. Deploy (SD + NFS) & Launch Telemetry
```bash
# Flash SD Card (replace /dev/sdX) and extract NFS rootfs
bzip2 -dc tmp/deploy/images/raspberrypi3/core-image-minimal-raspberrypi3.rootfs.wic.bz2 | sudo dd of=/dev/sdX conv=fdatasync bs=4M
sudo rm -rf /nfs/* && sudo tar xpf tmp/deploy/images/raspberrypi3/core-image-minimal-raspberrypi3.rootfs.tar.bz2 -C /nfs

# Deploy BME680 init script & Lighttpd web assets to /nfs
sudo cp ~/Yocto-RPi/meta-bootlinlabs/recipes-support/bme680-init/files/bme680 /nfs/etc/init.d/
sudo mkdir -p /nfs/www/pages/cgi-bin
sudo cp ~/Yocto-RPi/meta-bootlinlabs/recipes-support/web-telemetry/files/lighttpd.conf /nfs/etc/
sudo cp ~/Yocto-RPi/meta-bootlinlabs/recipes-support/web-telemetry/files/data.cgi /nfs/www/pages/cgi-bin/
sudo cp ~/Yocto-RPi/meta-bootlinlabs/recipes-support/web-telemetry/files/index.html /nfs/www/pages/
sudo chmod +x /nfs/etc/init.d/bme680 /nfs/www/pages/cgi-bin/data.cgi
```

On the Raspberry Pi (`ssh root@10.42.0.159`), enable the sensor service and start the web server:
```bash
update-rc.d bme680 defaults 99 && /etc/init.d/bme680 start
lighttpd -f /etc/lighttpd.conf
```
Open **`http://10.42.0.159/`** in your web browser to view the live telemetry dashboard.

## Full Project Report

For detailed screenshots, command outputs, and troubleshooting logs across every lab, refer to the complete PDF report included in this repository:

* [`docs/Rapport_Yocto_BENJOUAD_ELKOUSSAMI.pdf`](docs/Rapport_Yocto_BENJOUAD_ELKOUSSAMI.pdf)
