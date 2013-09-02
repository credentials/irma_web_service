Data directory for IRMA Web Service
===================================

This directory contains the data files. Make sure that:

 1. irma_configuration is linked into this directory
 2. irmaTube contains any movies that you want to show

data/irmaTube structure
-----------------------

This directory contains the movie trailer source files. Please ensure that

 * The movie is uploaded in both .webm and .mp6 to support all browsers.
   IRMATube expects both of these to be present. So you have movie.mp4 and
   movie.webm

 * In addition, create a file movie.access with the minimal viewing age on a
   the first line. Supported values are 12, 16, 18 and 0. The latter is used
   when no age restriction is required. The value you enter here is authorative.

 * Edit WebContent/fullDemo/irmaTube/content/movies.js to include the movie, see
   movies.js.example for an example. The age you enter here is only indicative,
   but should correspond to an appropriate age class for this movie.

 * Finally, add a cover image movie.jpg in WebContent/fullDemo/irmaTube/content/.

Only the files in data/irmaTube are protected against unauthorized access.
