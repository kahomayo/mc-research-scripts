# Fuse filesystem that disables renaming `level.*` files

Based on https://github.com/skorokithakis/python-fuse-sample, licensed under
BSD-2 Clause License, Copyright 2016 Stavros Korokithakis. See sample-LICENSE.

## Usage:

Download the `level-fuse.py` script somewhere onto your PC, e.g.
to `~/Downloads/fuse-no-level-dat/level-fuse.py`. If you don't want to
download the entire repository, you can click on the `level-fuse.py`
file above, click on the "Raw" button and then save the file with Ctrl+S.

Now open a terminal and enter the following commands (you don't need to type in
the lines starting with `#`, they are just explanations). This example assumes
that your world is `.minecraft/saves/world1`, replace that with the path to
your world folder.

```sh
# Install the tool that this script uses
pip3 install fusepy
# Create a new folder next to your world
mkdir .minecraft/saves/world1-mean-fs
# Run the script and tell it about your save and the new folder you made
python3 ~/Downloads/fuse-no-level-dat/level-fuse.py .minecraft/saves/world1 .minecraft/saves/world1-mean-fs
```

Run minecraft, open and close the newly created world (`world1-mean-fs` in
this example), then quit the filesystem with Ctrl+C. Minecraft will have
deleted the `level.dat` and `level.dat_old` of `world1`.

## No, mom, I swear it's not cheating

I don't consider this cheating as this filesystem doesn't violate the semantics
of a filesystem and all changes to the folder are done by minecraft itself.

All that this filesystem is doing, is disallowing files named `level.dat` from
being moved. The only difference from the sample are these lines:

```python3
    def rename(self, old, new):
        if 'level.dat' in old:
            print('blocking move', old, new)
            raise FuseOSError(errno.EACCES)
        else:
            return os.rename(self._full_path(old), self._full_path(new))
```

This is a perfectly legal filesystem, it doesn't change the contents of any
files. Filesystems blocking a move happens all the time, e.g. when the user
doesn't have permissions. This filesystem replicates that exact scenario, and
the error can be handled by programs in exactly the same way. If a program cared
that a move failed, it would respond to this error.

Filesystems are always allowed to produce errors, this one just produces them in
a specific scenario. It is just as much a normal filesystem as NTFS or ext4.
This filesystem also doesn't change how directories or files are stored, it
doesn't delete or create any files on its own. The only difference is that it is
selective about which renames it allows.

The reason that this is useful, is that Minecraft ignores errors when renaming
files. If `level.dat` -> `level.dat_old` fails, Minecraft just continues with
the next command, which is deleting `level.dat`.

