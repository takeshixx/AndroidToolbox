#!/bin/sh
# Put an APK into a raw ext4 system image (SP Flash Tool readback of 'system') as
# /system/priv-app/AndroidToolbox/AndroidToolbox.apk, root:root 0644, labelled like the stock priv-apps.
# Usage: inject-apk.sh system.img AndroidToolbox-system-platform.apk
set -eu
IMG=$1 APK=$2
E2=${E2FSPROGS:-/opt/homebrew/opt/e2fsprogs/sbin}
CTX='u:object_r:system_file:s0'

# The image must be exactly the filesystem (0xA0000000 on DY8321); the 2018/19 dump's ROM_19_system is longer.
fs=$("$E2/dumpe2fs" -h "$IMG" 2>/dev/null | awk -F: '/^Block count/{c=$2} /^Block size/{s=$2} END{print c*s}')
img=$(wc -c < "$IMG" | tr -d ' ')
[ "$fs" = "$img" ] || { echo "image is $img bytes, filesystem is $fs: truncate -s $fs $IMG" >&2; exit 1; }
# Android make_ext4fs images always trip "Padding at end of inode bitmap"; anything else means stop.
fsck() { "$E2/e2fsck" -fn "$IMG" 2>&1 | grep 'Fix? no' | grep -v 'Padding at end of inode bitmap' || true; }
[ -z "$(fsck)" ] || { echo "e2fsck errors on the input image; not touching it:" >&2; fsck >&2; exit 1; }

D=priv-app/AndroidToolbox F=$D/AndroidToolbox.apk
"$E2/debugfs" -w -f - "$IMG" <<CMDS 2>&1 | grep -v -E '^debugfs|File not found|does not exist' || true
rm $F
rmdir $D
mkdir $D
write $APK $F
sif $D mode 040755
sif $F mode 0100644
sif $D uid 0
sif $D gid 0
sif $F uid 0
sif $F gid 0
ea_set $D security.selinux "$CTX\\000"
ea_set $F security.selinux "$CTX\\000"
CMDS

[ -z "$(fsck)" ] && echo "fsck clean (apart from the stock bitmap padding)"
"$E2/debugfs" -R "stat $F" "$IMG" 2>/dev/null | grep -E 'Mode|User|Size:|security' 
"$E2/debugfs" -R "ea_list $D" "$IMG" 2>/dev/null
"$E2/debugfs" -R "dump $F /dev/stdout" "$IMG" 2>/dev/null | cmp - "$APK" && echo "APK content identical"
