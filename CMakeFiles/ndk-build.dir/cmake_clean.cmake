FILE(REMOVE_RECURSE
  "gen"
  "bin"
  "obj"
  "libs"
  "CMakeFiles/ndk-build"
)

# Per-language clean rules from dependency scanning.
FOREACH(lang)
  INCLUDE(CMakeFiles/ndk-build.dir/cmake_clean_${lang}.cmake OPTIONAL)
ENDFOREACH(lang)
