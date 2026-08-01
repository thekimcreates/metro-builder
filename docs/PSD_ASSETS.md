# PSD assets for Beta 1

The first PSD implementation will use Tianjin Metro's installed resource namespace as the
temporary visual source. MetroBuilder will not hard-code the TJMetro renderer or copy its
Java implementation.

The asset layer will be designed around a MetroBuilder PSD pack identifier so custom textures
and opening/closing sounds can replace the temporary style later without rewriting the
precision engine or breaking saved worlds.

No TJMetro textures are bundled in this foundation milestone because the PSD renderer has not
been implemented yet.
