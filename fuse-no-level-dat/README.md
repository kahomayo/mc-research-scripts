# Fuse filesystem that disables renaming `level.*` files

Based on https://github.com/skorokithakis/python-fuse-sample, licensed under
BSD-2 Clause License, Copyright 2016 Stavros Korokithakis. See sample-LICENSE.

## Usage:

Install fusepy with `pip3 install fusepy`.

Assuming the world you use is saved at `a/world1` and your minecraft saves are
at `b`

```
mkdir `b/world1`
python3 level-fuse.py a/world1 b/world1
```

Run minecraft, play until b/world1 no longer contains a level.dat.  Quit the
filesystem with Ctrl+C.

## No, mom, I swear it's not cheating

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

The reason that this is useful, is that minecraft ignores errors when renaming
files. If `level.dat` -> `level.dat_old` fails, Minecraft just continues with
the next command, which is deleting `level.dat`.

In conclusion: I don't consider this cheating as this filesystem doesn't violate
the semantics of a filesystem and the relevant deletion is done by minecraft
itself.
