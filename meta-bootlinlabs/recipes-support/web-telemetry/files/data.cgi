#!/bin/sh

# HTTP Response Header
printf "Content-Type: application/json\r\n"
printf "Cache-Control: no-cache, no-store, must-revalidate\r\n\r\n"

# Identify the IIO device path
IIO_PATH="/sys/bus/iio/devices/iio:device0"

if [ -d "$IIO_PATH" ]; then
    # Read raw temperature (millidegrees Celsius) and pressure (kilopascals)
    RAW_TEMP=$(cat "$IIO_PATH/in_temp_input" 2>/dev/null || echo "0")
    RAW_PRESS=$(cat "$IIO_PATH/in_pressure_input" 2>/dev/null || echo "0")

    # Format values using awk (Busybox compatible)
    TEMP_C=$(awk -v t="$RAW_TEMP" 'BEGIN { printf "%.2f", t / 1000 }')
    PRESS_HPA=$(awk -v p="$RAW_PRESS" 'BEGIN { printf "%.2f", p * 10 }')
    
    STATUS="ok"
else
    STATUS="sensor_not_found"
    TEMP_C="0.00"
    PRESS_HPA="0.00"
fi

# Output JSON
cat << EOF
{
  "status": "$STATUS",
  "temperature": $TEMP_C,
  "pressure": $PRESS_HPA
}
EOF
