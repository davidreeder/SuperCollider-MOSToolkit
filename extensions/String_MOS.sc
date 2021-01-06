//
// String_MOS.sc
//
// NB  Beware race conditions with UNIX commands!
//
//
// PUBLIC METHODS--
//   minimize
//   elide
//
//   isDirectory  isFile        
//   pathExists    
//   isAbsolutePath  
//   readDirectory  
//   ls  la  lf
//   delete
//   sizeBytes
//
//   regexpMatch
//   singleQuote  doubleQuote  unquote  quoteSpaces
//   removeWhitespace
//   unixCmdStdOut
//
//   openURL  
//   openTextEdit  openTerminal  
//   open  openFolder
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//   pretty
//
//
// MOS DEPENDENCIES--
//   Log
//   Object_MOS
//   Parse
//   Z
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018-2020.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


+ String  {

  //------------------------------------------- -o--
  *mosVersion  { 
    var  extensionVersion  = "0.2";   //RELEASE
    ^this.mosVersionString(extensionVersion);
  }



  //------------------------------------------- -o--
  // Instance methods.

  //------------------ -o-
  minimize  {
    var  sizePrev  = -1,
         str       = this.replace("\n", " ").replace("\t", " ");

    while ({ sizePrev != str.size }, {
      sizePrev = str.size;
      str = str.replace("  ", " ");
    });

    ^str.replace(" = ", "=")
        .replace("=\\\\", "=\\")
        .replace("{ | ", "{ |")
        .replace(" | ", "| ")
        ;
  }


  //------------------ -o-
  elide  { |cutoffLength|
    cutoffLength = if (cutoffLength.isNil, { 128; },  //XXX
                                           { cutoffLength.max(1); } );

    if (this.size <= cutoffLength, { ^this; });

    ^this[ (0..(cutoffLength-1)) ].join ++ "...";
  }


  //------------------ -o-
  isDirectory  { ^(\directory  == File.type(this.standardizePath)); }
  isFile       { ^(\regular    == File.type(this.standardizePath)); }

  pathExists   { ^(\not_found  != File.type(this.standardizePath)); }

  isAbsolutePath  { ^($/ == this[0]); }

  //withAbsolutePath  { ^PathName.new(this.standardizePath).absolutePath; }



  //------------------ -o-
  readDirectory  { |suffix=nil, lsArgs=""|
    var  cmd, list;

    cmd = ("ls -1 " ++lsArgs++ " %/%  2>/dev/null")
              .format(this.standardizePath.shellQuote, suffix ?? "");

    list = cmd.unixCmdGetStdOut.split($\n).as(Array);

    ^list;
  }


  //
  ls  { |suffix=nil, lsArgs=""|    
    ^("%\n%").format(
                Log.info(thisFunction, "ls % %/%", lsArgs, this, suffix ?? ""),
                Dump.collectionInColumns(this.readDirectory(suffix, lsArgs))
              );
  }

  la  { |suffix=nil|  ^this.ls(suffix, "-aF"); }

  lf  { |suffix=nil|  ^this.ls(suffix, "-F"); }


  //------------------ -o-
  delete  { |verbose|
    var  path  = this.standardizePath,
         rval  = File.delete(path);

    //DEFAULTS.
    verbose = verbose ?? true;

    //
    if (verbose, {
      if (rval, {
        Log.info(thisFunction, "DELETED object at path.  (%)", path).postln; 
      }, {
        Log.error(thisFunction, 
                    "FAILED to DELETE object at path.  (%)", path).postln; 
      });
    });

    ^rval;
  }


  //------------------ -o-
  sizeBytes  { ^File.size(this); }  //TBDFIX  right place for this?



  //------------------ -o-
  // RETURN: true/false
  //
  regexpMatch  { | pattern, casei, start, end
                 |
    casei    = casei    ?? true;
    start    = start    ?? 0;
    end      = end      ?? this.size;

    if (pattern.isNil, { ^true; });  // .*

    if (casei, {
      ^pattern.asString.toLower.matchRegexp(this.toLower, start, end);
    }, {
      ^pattern.asString.matchRegexp(this, start, end);
    });
  }



  //------------------ -o-
  singleQuote  { ^this.shellQuote; }

  doubleQuote  { ^this.quote; }

  quoteSpaces  { ^this.replace(" ", "\\ "); }

  unquote  { 
    ^this
       .replace($".asString, "")
       .replace($'.asString, "")
       .replace("\\ ", " "); 
  }


  //------------------ -o-
  removeWhitespace  { 
    ^this
       .replace(" ", "")
       .replace("\t", "");
  }



  //------------------ -o-
  unixCmdStdOut  { ^this.unixCmdGetStdOut.stripWhiteSpace; }



  //------------------ -o-
  openURL       { ^Z.openURL(this); }

  openTextEdit  { ^Z.openTextEdit(this); }

  openTerminal  { ^Z.openTerminal(this); }

  //
  open          { |folder|  ^Z.open(this, folder); }

  openFolder    { ^Z.open(this, folder:true); }




  //------------------------------------------- -o--
  // MOS protocol methods

  //------------------ -o-
  // NB  Do not .minel strings when compileString is false.
  //
  pretty  { | pattern, elisionLength, depth,
              casei, compileString, indent, initialCallee, 
              enumerate, bulletIndex, enumerationOffset,
              minimumSpacerSize, nolog, shortVersion
            |
    //DEFAULT.
    compileString = compileString ?? false;

    if (Parse.isInstanceOfClass(thisFunction, 
          compileString, "compileString", Boolean).not,  { ^nil; });


    //
    if (this.regexpMatch(pattern, casei).not, { ^""; });

    if (compileString, { ^this.csminel(elisionLength); });

    ^this;
  }


} //String

