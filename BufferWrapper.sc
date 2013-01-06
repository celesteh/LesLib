BufferWrapper
{

  // a class to hold a data about a buffer, including the bufnum on the server
  // the number of frames and the samplerate

	var <bufnum;
	var <sampleRate = 44100;
	var <numFrames = -1;
	var <dur;
 	var buf;
 	var server;
 
 	*new { arg srv, name;
 
 		^super.new.init(name, srv);
 	}
 
 	init { arg name, srv;
 	
 		// get the header information by opneing a soundfile
 		var soundFile;
 		soundFile = SoundFile.new;
 		soundFile.openRead(name);
 		sampleRate = soundFile.sampleRate;
 		numFrames = soundFile.numFrames;
 		soundFile.close;
 		
 		dur = numFrames / sampleRate;
 		// then read the file as a buffer ot the server
 		
 		server = srv;
 		buf = Buffer.read(srv, name);
 		
 		bufnum = buf.bufnum;
 	}
 
  	
  	free {

		// de-allocate memory on the server
  	
  		buf.free;
  	}

}
 