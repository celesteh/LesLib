P5Glove
{

  // a class to talk to the p5 glove.  You can poll it, or you can have it alert your fucntions
  
  var <name;
  var <a_button, <b_button, <c_button;
  var <thumb, <index, <middle, <ring, <pinkie;
  var <x_pos, <y_pos, <z_pos;
  
  var >pos_func, >finger_func, >button_func;
  var >x_func, >y_func, >z_func;
  var >thumb_func, >index_func, >middle_func, >ring_func, >pinkie_func;
  var >a_func, >b_func, >c_func;
  
  var <>pos_threshold, <>finger_threshold, <>z_pos_threshold;
  
  var a_last, b_last, c_last;
  var thumb_last, index_last, middle_last, ring_last, pinkie_last;
  var x_last, y_last, z_last;
  
   	*new { arg p_thresh = 10, f_thresh = 5, z_thresh = 20;
 
 		^super.new.init(p_thresh, f_thresh, z_thresh);
 	}


     init {arg p_thresh = 10, f_thresh = 5, z_thresh = 20;
     
     	pos_threshold = p_thresh;
     	finger_threshold = f_thresh;
     	z_pos_threshold = z_thresh;
     
         	OSCresponder(nil,'/p5glove_data',
         		{arg time, responder, message; this.respond(time, responder, message)}).add;
        	
     }
     
     do_button { arg current, last, func;
         var changed;
     
     	changed = false;
         if (current != last, {
             changed = true;
             if (func != nil, {
                 func.value(current);
             });
         });
         ^changed;
     }
     
     
     do_finger { arg current, last, func;
         var changed;
     
     	changed = false;
         if ((current==nil) && (last == nil), {}, {
           if ((current == nil) || (last == nil), {
             changed = true;
             if (func != nil, {
                 func.value(current);
             });
           }, {              
             if ((current - last).abs > finger_threshold, {
               changed = true;
               if (func != nil, {
                 func.value(current);
               });
             });
           });  
         });
         ^changed;
     }
     
     do_position { arg current, last, func, thresh;
         var changed;
         
         changed = false;
         
         if ((current==nil) && (last == nil), {}, {
           if ((current == nil) || (last == nil), {
             changed = true;
             if (func != nil, {
                 func.value(current);
             });
           }, {              
             if ((current - last).abs > thresh, {
               changed = true;
               if (func != nil, {
                 func.value(current);
               });
             });
           }); 
         });
         ^changed;
     }

 

     
     respond {
             arg time, responder, message;
             var changed;
        if (message != nil, { 
          //"not nil".postln;    
          if (message.at(0) == '/p5glove_data', {
               // "hello".postln;
               // message.at(0).postln; // Name
               // message.at(1).postln; // A button
               // message.at(2).postln; // B button
               // message.at(3).postln; // C button
               // message.at(4).postln; // Thumb
               // message.at(5).postln; // First finger
               // message.at(6).postln; // Middle finger
               // message.at(7).postln; // Third finger
               // message.at(8).postln; // Pinky
               // message.at(9).postln; // Vertile
               // message.at(10).postln; // Horizontal
               // message.at(11).postln; // z-axis
               
               name = message.at(0);
               
               // do buttons
               
               changed = false;
               a_button = message.at(1);
               if (this.do_button(a_button, a_last, a_func), {
               	a_last = a_button;
               	changed = true;
               });

               b_button = message.at(2);
               if (this.do_button(b_button, b_last, b_func), {
               	b_last = b_button;
               	changed = true;
               });

               c_button = message.at(3);
               if (this.do_button(c_button, c_last, c_func), {
               	c_last = c_button;
               	changed = true;
               });
               
               if (changed && (button_func != nil), {
               	button_func.value(a_button, b_button, c_button);
               });
               
               
               // do fingers
               
               changed = false;
               thumb = message.at(4); 
               if(this.do_finger(thumb, thumb_last, thumb_func), {
                 thumb_last = thumb;               
                 changed = true;
                });
               
               index = message.at(5);
               if(this.do_finger(index, index_last, index_func), {
			   index_last = index; 
                 changed = true;
                });
               
               middle = message.at(6);
                if(this.do_finger(middle, middle_last,  middle_func), {               
                 middle_last = middle;
                 changed = true;
                });
              
               ring = message.at(7); 
               if(this.do_finger(ring, ring_last, ring_func), {
                 ring_last = ring;
                 changed = true;
                });
               
               pinkie = message.at(8);
               if(this.do_finger(pinkie, pinkie_last, pinkie_func), {
                 pinkie_last = pinkie;
                 changed = true;
                });
                
                if (changed && (finger_func != nil), {
                	finger_func.value(thumb, index, middle, ring, pinkie);
                });
                
                
                // position
               
               changed = false;
               x_pos = message.at(9);
               if(this.do_position(x_pos, x_last, x_func, pos_threshold), {
                 x_last = x_pos;
                 changed = true;
                });

               y_pos = message.at(10);
               if(this.do_position(y_pos,  y_last,  y_func, pos_threshold), {
                 y_last = y_pos;
                 changed = true;
                });
               
               z_pos = message.at(11);
               if(this.do_position(z_pos, z_last, z_func, z_pos_threshold), {
                 z_last = z_pos;
                 changed = true;
                });
                
                if (changed && (pos_func != nil), {
                  pos_func.value(x_pos, y_pos, z_pos);
                 });
          });
        });
    }
    
}
