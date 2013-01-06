BBBufferTool : BufferTool
{

  // a minor extension to BuferTool, for historical reasons, to use with BBBCutBuffers
 	
 	
 	*alloc { arg srv, dur,  channels = 2, sampleRate = 44100, action, 
 			class = BBCutBuffer;
 	
 		^super.new.rec_init(srv, 1, dur, channels, sampleRate, action, class);
 	}

 	
 	*grain { arg srv, buffer, startFrame, endFrame, sampleRate, synthDefName, bufnum, bufamp, 
 			class = BBCutBuffer;
 	
 		^super.new.grain_init(srv, buffer, startFrame, endFrame, sampleRate, 
 							synthDefName, bufnum, bufamp, class);
 	}
 	
 	
 	*new { arg srv, in= 1, dur,  channels = 2, sampleRate = 44100, action, 
 			class = BBCutBuffer;
 	
 		^super.new.rec_init(srv, in, dur, channels, sampleRate, action, class);
 	}
 
 	*open { arg srv, name, threshold = 0, length = 0, take_last = false, min = 4410, action,
 			class = BBCutBuffer, beatlength = 8;
 
 		var act;
 
 		act = {|btool| 
 		
 			action.value(btool); 
 			btool.respondsTo('beatlength_').if ({
 				btool.beatlength = beatlength;
 			});
 		}
 
 		^super.new.open_init(name, srv, act, class);
 	}
 	
 	
 	
 	beatlength_{ arg length;
 	
 		buf.respondsTo('beatlength_').if ({
	  		buf.beatlength = length;
	  	});
 	}
 	

}
 