//
// Window_MOS.sc
//
//
// PUBLIC METHODS--
//   *screenHeight  *screenWidth
//   *offsetFromDisplayEdge  
//   *offsetFromDisplayTop  *offsetFromDisplayRight
//   *findWindowByName
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//
//
// MOS DEPENDENCIES--
//   Log
//   Object_MOS
//   Parse
//   String_MOS
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018-2020.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


+ Window  {

  //------------------------------------------- -o--
  *mosVersion  { 
    var  extensionVersion  = "0.1";   //RELEASE
    ^this.mosVersionString(extensionVersion);
  }




  //------------------------------------------- -o--
  // Class methods.

  //------------------ -o-
  // Origin is lower-left.
  // Rect properties: left, top, width, height.
  // 
  *screenHeight  { ^Window.availableBounds.height; }
  *screenWidth   { ^Window.availableBounds.width; }
        // VERSUS Window.screenBounds


  //------------------ -o-
  *offsetFromDisplayEdge  { | offset, edgeName, edgeValue
                            |
    offset = offset ?? 0;

    if (Parse.isInstanceOfClass(
                  thisFunction, offset, "offset", Number).not, { ^(-1); });

    //
    if (offset > edgeValue, {
      Log.error( thisFunction, 
                 "offset (%) CANNOT exceed % (%).", offset, edgeName, edgeValue
               ).postln; 
      ^0;
    });

    ^(edgeValue - offset);
  }


  //
  *offsetFromDisplayTop  { |offset|
    ^this.offsetFromDisplayEdge(offset, "screenHeight", this.screenHeight);
  }

  //
  *offsetFromDisplayRight  { |offset|
    ^this.offsetFromDisplayEdge(offset, "screenWidth", this.screenWidth);
  }


  //------------------ -o-
  // Find a window, if it exists.
  // Optionally move it to a new <windowRect>.
  // Optionally bring it to the foreground.
  //
  // NB  windowRect may contain negative values  indicating that the
  //     current value should be used.
  //
  // RETURN:  First window with name <windowName>.
  //
  *findWindowByName  { | windowName,
                         windowRect,
                         bringToFront
                       |
    var  window;

    if (Parse.isInstanceOfClass(thisFunction, 
                  windowName, "windowName", String).not, { ^nil; });

    if (Parse.isInstanceOfClass(thisFunction, 
                  windowRect, "windowRect", Rect, true).not, { ^nil; });

    if (Parse.isInstanceOfClass(thisFunction, 
                  bringToFront, "bringToFront", Boolean, true).not, { ^nil; });


    //
    Window.allWindows.do({ |win|
      //if (windowName == win.name, { window = win; });
      if (win.name.regexpMatch(windowName), { window = win; });
    });

    if (window.isNil, { ^nil });

    
    //
    if (windowRect.notNil,   
    { 
      if (windowRect.left < 0,   {windowRect.left    = window.bounds.left; });
      if (windowRect.top < 0,    {windowRect.top     = window.bounds.top; });
      if (windowRect.width < 0,  {windowRect.width   = window.bounds.width; });
      if (windowRect.height < 0, {windowRect.height  = window.bounds.height; });

      window.bounds = windowRect; 
    });

    if (bringToFront.notNil, { window.front; });

    ^window;
  }


} //Window

