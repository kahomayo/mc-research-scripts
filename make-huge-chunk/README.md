# Produce worlds with large b1.2_02 chunks

The chunk `2/2/c.2.2.dat` is made oversized through various means. I don't think that JNBT perfectly replicates, how Minecraft serializes it's NBT, so there is some deviation in file sizes when opened in b1.2_02. Savestating happens starting in b1.3

JNBT1.4 (https://github.com/Morlok8k/JNBT) is in the libs folder, needs to be installed with `mvn install-file` before building. License is MIT.
