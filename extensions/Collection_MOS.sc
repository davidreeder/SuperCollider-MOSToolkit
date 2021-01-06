//
// Collection_MOS.sc
//
//
// PUBLIC METHODS--
//   argsp
//   args  argsnl
//   what
//
//   maxLength
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//
//
// MOS DEPENDENCIES--
//   Dump
//   Object_MOS
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018-2020.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


+ Collection  {

  //------------------------------------------- -o--
  *mosVersion  { 
    var  extensionVersion  = "0.1";   //RELEASE
    ^this.mosVersionString(extensionVersion);
  }



  //------------------------------------------- -o--
  // Public instance methods.
  
  //--------------------- -o-
  argsp  { |logContext, indent|  
    var  str  = Dump.argsp(this, indent);
    ^Log.debug(logContext, str); 
  }

  args    { |logContext|  ^Dump.args(this, logContext); }

  argsnl  { |logContext|  ^Dump.argsnl(this, logContext); }


  //
  what    { ^Dump.what(this); }



  //--------------------- -o-
  maxLength  { ^this.maxItem({ |elem|  elem.asString.size; }).size; }


} //Collection

