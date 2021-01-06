//
// Float_MOS.sc
//
//
// MOS PROTOCOL SUPPORT--
//   pretty
//   *mosVersion
//
//
// MOS DEPENDENCIES--
//   Object_MOS
//   String_MOS
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018-2020.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


+ Float  {

  //------------------------------------------- -o--
  *mosVersion  { 
    var  extensionVersion  = "0.1";   //RELEASE
    ^this.mosVersionString(extensionVersion);
  }



  //------------------------------------------- -o--
  // Instance methods.

  pretty  {     | pattern, elisionLength, depth,
                  casei, compileString, indent, initialCallee, 
                  enumerate, bulletIndex, enumerationOffset,
                  minimumSpacerSize, nolog, shortVersion
                |
    var  str  = this.asString;

    if (str.regexpMatch("\\.").not, {
      str = str ++ ".0";
    });

    if (str.regexpMatch(pattern, casei).not, { ^""; });

    ^str;
  }


} //Float

