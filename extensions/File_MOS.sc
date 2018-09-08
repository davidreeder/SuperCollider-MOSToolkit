//
// File_MOS.sc
//
//
// PUBLIC METHODS--
//   *pathToNewFile
//     *pathToDesktopFile
//     *pathToUserTmpFile
//
//   *date
//     *dateModification  
//     *dateCreation  
//   *isFileNewer
//
//   *size
//
//   *findFilesUnderArrayOfDirectoriesWithSuffixPattern 
//   *createDirectory
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//
//
// MOS DEPENDENCIES--
//   Date_MOS
//   Dump
//   Log
//   Object_MOS
//   Parse
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


+ File  {

  //------------------------------------------- -o--
  *mosVersion  { 
    var  extensionVersion  = "0.2";   //RELEASE
    ^this.mosVersionString(extensionVersion);
  }




  //------------------------------------------- -o--
  // Class methods.

  //------------- -o-
  *pathToNewFile  { |filename, suffix, directory, datestamp|
    var  pathname,
         datestampValue;

    //DEFAULTS.
    filename   = filename   ?? "unnamed";
    suffix     = suffix     ?? "txt";
    directory  = directory  ?? "~/";
    datestamp  = datestamp  ?? true;

    suffix = "." ++ suffix;


    //
    datestampValue = if (datestamp, { "--" ++ Date.datestamp; }, { ""; });

    pathname = ("%%%%").format(
                                   directory.standardizePath, 
                                   filename, 
                                   datestampValue, 
                                   suffix
                                 );
    //
    ^pathname;
  }


  //------------- -o-
  *pathToDesktopFile  { |filename, suffix, datetamp|
    ^this.pathToNewFile(filename, suffix, "~/Desktop/", datetamp);
  }

  //------------- -o-
  *pathToUserTmpFile  { |filename, suffix, datetamp|
    ^this.pathToNewFile(filename, suffix, "~/tmp/", datetamp);
  }



  //------------------------ -o-
  // Use UNIX "date" to convert date string from UNIX "ls" to seconds.
  //
  // ARGS:
  //   (String) path to file
  //   (String) args for UNIX ls  [DEFAULT: -lTt (modification time)]
  //
  // RETURN: 
  //   (Integer) date in seconds since epoch
  //     -OR-
  //   Log.error.
  //
  // VERSUS  File.mtime() for modification date.
  //
  // NB  awkscript ASSUMES bash.
  //
  // NB  WORKING COMMANDLINE  
  //       date -j -f '%b %d %T %Y' \
  //         "$(echo $(ls -lTt './somefile' | awk '{ print $6, $7, $8, $9 } '))"
  //         '+%s'
  //
  // XXX  Fragile!
  //
  *date  { |path, lsArgs="-dlTt"|

    var  awkscript,
         //dateConversionFormat  = "date -j -f '%b %d %H:%M:%S %G' ",
                // NB  Valid prior to OSX 10.12 upgrade.
         dateConversionFormat  = "date -j -f '%b %d %T %Y' ",
         suffix                = " '+%s'  2>/dev/null";

    var  cmd,
         rval  = 0;


    //
    if (path.isNil, { ^Log.error(thisFunction, "<path> is nil"); });


    //DEFAULTS.
    path = path.standardizePath;

    awkscript = ("%$(echo $(ls % '%' | awk '{ print $6, $7, $8, $9 } '))%")
                    .format("\"", lsArgs, path, "\"");

    cmd = dateConversionFormat ++ awkscript ++ suffix;


    // XXX  Poll unixCmdGetStdOut until it returns non-empty value.
    //
    if (this.exists(path), {
      while ( {0 == rval}, { rval = cmd.unixCmdGetStdOut.asInt; });
    }, {
      ^Log.error(thisFunction, "File does not exist.  (%)", path);
    });


    ^rval;
  } 


  //
  *dateModification  { |path, lsArgs="-lTt"|
    ^this.date(path, lsArgs);
  }

  *dateCreation  { |path, lsArgs="-lTU"|
    ^this.date(path, lsArgs);
  }



  //------------------------ -o-
  // Per *.dateModification.
  //
  *isFileNewer  { | firstpath, 
                    secondpath
                  |
    var  firstpathExists   = File.exists(firstpath.standardizePath),
         secondpathExists  = File.exists(secondpath.standardizePath);

    if (firstpathExists.not && secondpathExists.not, {
      ^Log.error(thisFunction, "Neither file exists."); 
    });

    if (firstpathExists.not,   { ^false; });
    if (secondpathExists.not,  { ^true; });

    ^(File.dateModification(firstpath) > File.dateModification(secondpath));
  }



  //------------------------ -o-
  *size  { |path|  
    if (path.isNil, { ^Log.error(thisFunction, "<path> is nil"); });

    //DEFAULTS.
    path = path.standardizePath;

    if (File.exists(path).not, {
      ^Log.error(thisFunction, "File does NOT EXIST.  (%)", path); 
    });


    //
    ^(Dump.sizeInBytes(File.fileSize(path).asFloat));
  }



  //------------------------ -o-
  *findFilesUnderArrayOfDirectoriesWithSuffixPattern  { 
                | arrayOfDirectories, suffix, 
                  pattern, casei,
                  returnFullPath
                |
    var  arrayOfFiles  = Array.new,
         suffixString;
    
    var  pathname,
         pathnameForPatternMatch,
         suffixMatches;


    //
    suffixString    = if (suffix.notNil, { suffix.asString; });
    pattern         = if (pattern.isNil, { ".*"; }, { pattern.asString; });
    casei           = casei ?? false;
    returnFullPath  = returnFullPath ?? false;

    if (String == arrayOfDirectories.class, {
      arrayOfDirectories = [ arrayOfDirectories ];
    });

    if (Array != arrayOfDirectories.class, {
      ^Log.error(thisFunction, "<arrayOfDirectories> is not an Array."); 
    });


    //
    arrayOfDirectories.do { | directory
                            |
      if (directory.isDirectory.not, {
        Log.error(thisFunction, 
                    "Path IS NOT A DIRECTORY.  Skipping...  (%)", directory
                 ).postln; 
        directory = nil;
      });

      if (directory.notNil, {
        PathName.new(directory).filesDo { | fileobj
                                          |
          pathname       = fileobj.fullPath;
          suffixMatches  = false;

          if (returnFullPath.not, {
            pathname = pathname.replace(directory ++ "/", "");
          });

          if (    suffix.isNil
               || (    suffix.notNil 
                    && fileobj.extension
                         .regexpMatch(suffixString, casei:casei) ),
          {
            suffixMatches = true;
          });

          if (suffixMatches, {
            if (fileobj.extension.size > 0, {
              pathnameForPatternMatch = 
                pathname[0..(pathname.size - fileobj.extension.size - 2)];
            }, {
              pathnameForPatternMatch = pathname;
            });

            if (pathnameForPatternMatch.regexpMatch(pattern, casei:casei), {
              arrayOfFiles = arrayOfFiles.add(pathname);
            });
          });

        }; //fileobj
      });
    }; //directory


    //
    if (arrayOfFiles.size > 0, { 
      ^arrayOfFiles;
    }, {
      ^nil; 
    });

  } // *findFilesUnderArrayOfDirectoriesWithSuffixPattern 



  //------------------------ -o-
  // RETURNS  true on success, else false.
  //
  *createDirectory  { | path
                      |
    var  doesFileExist,
         pathObj,
         rval;

    //
    if (Parse.isInstanceOfClass(
              thisFunction, path, "path", String).not, { ^false; });


    //DEFAULTS.
    path = path.standardizePath;

    pathObj = PathName.new(path.standardizePath);


    // Test for existence.
    //
    rval = try { File.exists(path); } { |error|  error; };

    if (PrimitiveFailedError == rval.class, {
      "".postln;
      Log.error(thisFunction, "COULD NOT ACCESS path.  (%)", path).postln; 
      ^false;
    });

    if (rval, 
    {
      if (pathObj.isFolder.not, {
        Log.error( thisFunction, 
                   "path exists but IS NOT a DIRECTORY.  (%)", path
                 ).postln; 
        ^false;
      });

      ^true;
    });


    // Create new folder.
    //
    File.mkdir(path); 

    if (pathObj.isFolder.not, 
    {
        Log.error( thisFunction, 
                   "Could NOT CREATE DIRECTORY for path.  (%)", path
                 ).postln; 
        ^false;
    });

    ^true;
  }


} //File

