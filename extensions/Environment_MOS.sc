//
// Environment_MOS.sc
//
//
// PUBLIC METHODS--
//   *pushNew
//   (*)pseudoObject
//   *top  *current
//
//   *ment
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//
//
//
// MOS DEPENDENCIES--
//   Dictionary_MOS
//   Object_MOS
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018-2020.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


+ Environment  {

  //------------------------------------------- -o--
  *mosVersion  { 
    var  extensionVersion  = "0.1";   //RELEASE
    ^this.mosVersionString(extensionVersion);
  }



  //------------------------------------------- -o--
  // Class methods.

  //--------------------- -o-
  *pushNew  { 
    Environment.make.push;  
    ^("Environment.stack.size=%").format(Environment.stack.size);
  }


  //--------------------- -o-
  // NB  Argument list for functions within Object/Environment must
  //     always lead with "self", eg:
  //        e.f = { |self, other, args |  doSomething; }
  //
  *pseudoObject  {
    var  e  = ();
    e.know = true;
    ^e;
  }

  pseudoObject  { ^this.class.pseudoObject(); }  //INSTANCE

 
  //--------------------- -o-
  *top      { ^topEnvironment; }  

  *current  { ^currentEnvironment; }



  //--------------------- -o-
  *ment  { | pattern, elisionLength, 
             casei, compileString, indent, initialCallee, 
             title, minimumSpacerSize
           |
    title = title ?? "currentEnvironment";

    ^currentEnvironment.ment( pattern, elisionLength, 
                              casei, compileString, indent, initialCallee, 
                              title, minimumSpacerSize
                            );
  }  // *ment


} //Environment

