#!/bin/sh
# Host-side check: does the tablet expose an ADB USB interface (class/subclass/protocol 255/66/1)?
#   no ADB interface  -> device side (adb not in sys.usb.config/state); run Diagnose in the app
#   ADB interface, adb shows nothing/unauthorized -> host side or RSA prompt on the tablet
echo "== adb devices -l =="
adb devices -l 2>&1

echo "== USB interfaces (class/subclass/protocol) =="
if [ "$(uname)" = Darwin ]; then
  ioreg -r -c IOUSBHostInterface -l -w0 | awk -F' = ' '
    /\+-o /               { if (n) print n, c "/" s "/" p (c==255 && s==66 && p==1 ? "  <== ADB" : ""); n=$0; sub(/^.*\+-o /,"",n); sub(/@.*/,"",n); c=s=p="" }
    /"bInterfaceClass"/   { c=$2 }
    /"bInterfaceSubClass"/{ s=$2 }
    /"bInterfaceProtocol"/{ p=$2 }
    END                   { if (n) print n, c "/" s "/" p (c==255 && s==66 && p==1 ? "  <== ADB" : "") }'
  echo "== USB serial ports (toolbox 'Serial shell': screen <port> 115200) =="
  ls /dev/cu.usbmodem* 2>/dev/null || echo "none (set 'USB: mtp,acm' in the toolbox)"
else
  for d in /sys/bus/usb/devices/*:*; do
    c=$((0x$(cat "$d/bInterfaceClass"))) s=$((0x$(cat "$d/bInterfaceSubClass"))) p=$((0x$(cat "$d/bInterfaceProtocol")))
    v=$(cat "$d/../idVendor"):$(cat "$d/../idProduct")
    echo "$v $(cat "$d/interface" 2>/dev/null) $c/$s/$p$([ "$c/$s/$p" = 255/66/1 ] && echo '  <== ADB')"
  done
  ls /dev/ttyACM* 2>/dev/null
  echo "(Linux: a listed ADB interface but 'no permissions' in adb means a missing udev rule for that vendor id)"
fi
