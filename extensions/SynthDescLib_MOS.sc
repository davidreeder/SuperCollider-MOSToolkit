//
// SynthDescLib_MOS.sc
//
//
// PUBLIC METHODS--
//   *listOfSynths  [*los]
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//
//
// MOS DEPENDENCIES--
//   Object_MOS
//   Parse
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


+ SynthDescLib  {

  //------------------------------------------- -o--
  *mosVersion  { 
    var  extensionVersion  = "0.1";   //RELEASE
    ^this.mosVersionString(extensionVersion);
  }



  //------------------------------------------- -o--
  // Class methods.

  //------------------------- -o-
  *listOfSynths  { | regexp,
                     library
                   |
    var  synthDescLibrary,
         synthDescList,
         synthArray;


    //DEFAULTS.
    regexp   = regexp   ?? "";
    library  = library  ?? \global;

    //
    if (Parse.isInstanceOfClass(thisFunction, 
                  regexp, "regexp", [String, Symbol]).not,      { ^nil; });

    if (Parse.isInstanceOfClass(thisFunction, 
                  library, "library", Symbol).not,              { ^nil; });


    //
    synthDescLibrary = SynthDescLib.all.keys.findMatch(library);
    if (synthDescLibrary.isNil, { ^nil; });

    synthDescList = SynthDescLib.all[synthDescLibrary].synthDescs;

    synthArray = 
      synthDescList.keys.select({ |elem|  
        elem.asString.regexpMatch(regexp); 
      }).asArray.sort;


    //
    ^synthArray;
  }

  
  //ALIAS
  *los  { ^this.listOfSynths; }   


} //SynthDescLib

