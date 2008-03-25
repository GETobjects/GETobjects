ofs.config
**********

The configuration subsystem inspired by Apache. It starts reading configurations
from the root of the OFS tree and merges them along the containment path.
Note that a configuration is just a key/value Map and is NOT tied to the
HtAccess subsystem. Hence a configuration can be stored by other means, its just
the lookup and assembly of the configuration which is standardized.

Technically this packages could be moved out of OFS, no specific tieds.

As an example, the lookup of a configuration object goes against the 'config'
name. Whether the config is stored in an config.htaccess, config.plist or
config.xml doesn't matter.
The 'config' object just needs to conform to the IJoConfigurationProvider
interface to create a Map representation of the config in the given lookup
context.
 