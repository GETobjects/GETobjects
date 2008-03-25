This small example allows the user to upload an image file. The file is stored
in a session and then scaled using Java2D to thumbnails.
The thumbs are then displayed using <img> tags.

Note: you should run Java in 'headless' mode :-) This is done by specifying:

  -Djava.awt.headless=true
  
in the VM parameters.
