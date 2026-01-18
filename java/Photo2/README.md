Multi-purpose photo handling.  Checks for .jpg files in the source directory hierarchy existing in the destination and compares their sha - when different a modified name is used to copy

When handling photos from an android phone (under linux). First find the phone details after plugging it into a USB cable and enabling usage for file transfer by running

    'ls -l /run/user/1000/gvfs'

which returns e.g.

    'total 0
    dr-x------ 1 ian-brown ian-brown 0 Jan  1  1970 'mtp:host=SAMSUNG_SAMSUNG_Android_R5CY12KB0ZB'
    drwx------ 1 ian-brown ian-brown 0 Feb 19  2022 'smb-share:server=nas.local,share=photo'
    '

then edit the run configuration in intellij to include the program arguments e.g.

    "/run/user/1000/gvfs/mtp:host=SAMSUNG_SAMSUNG_Android_R5CY12KB0ZB/Internal storage/DCIM"
    /run/user/1000/gvfs/smb-share:server=nas.local,share=photo/OriginalsFromCamera
    01/01/2025