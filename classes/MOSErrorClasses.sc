//
// MOSErrorClasses.sc
//
// Defines MOSError (subclass of Error) which has the following subclasses:
//   MOSErrorAssert
//   MOSErrorFatal
//   MOSErrorSpinlockTimeout
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


//---------------------------------------------- -o--
MOSErrorClasses : MobileSound
{
  classvar  classVersion  = "0.1";   //RELEASE

  *mosVersion  { ^super.mosVersion(classVersion); }
}




//---------------------------------------------- -o--
MOSError : Error
{
  errorString  { ^"MOS ERROR: " ++ what; }
}




//---------------------------------------------- -o--
MOSErrorAssert : MOSError  
{ 
  errorString  { ^"MOS ERROR ASSERT: " ++ what; }
}

MOSErrorFatal : MOSError
{ 
  errorString  { ^"MOS ERROR FATAL: " ++ what; }
}

MOSErrorSpinlockTimeout : MOSError
{ 
  errorString  { ^"MOS ERROR SPINLOCK TIMEOUT: " ++ what; }
}

