# Recipe created by recipetool
# This is the basis of a recipe and may need further editing in order to be fully functional.
# (Feel free to remove these comments when editing.)

# WARNING: the following LICENSE and LIC_FILES_CHKSUM values are best guesses - it is
# your responsibility to verify that the values are complete and correct.
SUMMARY = "Color Tetris clone for the terminal"
HOMEPAGE = "https://github.com/0xminik/ctris"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=0636e73ff0215e8d672dc4c32c317bb3 \
                    file://LICENSE;md5=2c1c00f9d3ed9e24fa69b932b7e7aff2"

SRC_URI = "git://github.com/0xminik/ctris.git;protocol=https;branch=master"

# Modify these as desired
PV = "1.0+git"
SRCREV = "9db20c4aa60589bc9ea1509d9c81519b1ccde526"

S = "${WORKDIR}/git"

# NOTE: this is a Makefile-only piece of software, so we cannot generate much of the
# recipe automatically - you will need to examine the Makefile yourself and ensure
# that the appropriate arguments are passed in.

DEPENDS = "ncurses"


do_compile () {
    oe_runmake CC="${CC}" CFLAGS="${CFLAGS}" LDFLAGS="${LDFLAGS}" LIBS="-lncurses ${LDFLAGS}"
}

do_install () {
    install -d ${D}${bindir}
    install -m 0755 ctris ${D}${bindir}/
}
