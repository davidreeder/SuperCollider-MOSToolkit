//
// Pattern_MOS.sc
//
//
// PUBLIC METHODS--
//   *verbose  [*v]
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//
//
// MOS DEPENDENCIES--
//   Object_MOS
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018-2020.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


+ Pattern  {

  //------------------------------------------- -o--
  *mosVersion  { 
    var  extensionVersion  = "0.1";   //RELEASE
    ^this.mosVersionString(extensionVersion);
  }




  //------------------------------------------- -o--
  // Instance methods.

  //------------------------ -o-
  *verbose  { |patternFunction, output="", verbose=true|
    ^Pif(Pfunc({ verbose; }), 
           patternFunction.trace(prefix:output), 
           patternFunction
        );
  }

  //
  *v  { |patternFunction, output="", verbose=false|  // ALIAS
    ^this.verbose(patternFunction, output, verbose); 
  }


} //Pattern

