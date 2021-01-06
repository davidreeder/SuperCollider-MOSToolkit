//
// SimpleNumber_MOS.sc
//
//
// PUBLIC METHODS--
//   roundPadRight
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


+ SimpleNumber  {

  //------------------------------------------- -o--
  *mosVersion  { 
    var  extensionVersion  = "0.1";   //RELEASE
    ^this.mosVersionString(extensionVersion);
  }




  //------------------------------------------- -o--
  // Instance methods.

  //----------------- -o-
  roundPadRight  { | roundingFactor
                   |
    var  roundedNumberString       = this.round(roundingFactor).asString,
         decimalIndex              = roundedNumberString.findBackwards("."),
         factorLength              = 0,
         sizeOfPaddedNumberString  = -1;


    //
    if ( (roundingFactor >= 1) || decimalIndex.isNil, {
      ^roundedNumberString; 
    });
    

    //
    while ( { roundingFactor < 1; }, {
      factorLength    = factorLength + 1;
      roundingFactor  = roundingFactor * 10;
    });

    sizeOfPaddedNumberString = (decimalIndex + 1) + factorLength;

    ^roundedNumberString.padRight(sizeOfPaddedNumberString, "0");   //XXX
  }


} //SimpleNumber

