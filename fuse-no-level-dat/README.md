# Fuse filesystem that disables renaming `level.*` files

Based on https://github.com/skorokithakis/python-fuse-sample, licensed under
BSD-2 Clause License, Copyright 2016 Stavros Korokithakis. See sample-LICENSE.

## Usage:

Install fusepy with `pip3 install fusepy`.

Assuming the world you use is saved at `a/world1`.

```
mkdir `a/world1-mean-fs`
python3 level-fuse.py a/world1 a/world1-mean-fs
```

Run minecraft, open and close world1-mean-fs. Quit the filesystem with Ctrl+C.
Minecraft will have deleted the `level.dat` and `level.dat_old` of world1.

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

