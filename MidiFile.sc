/*
MidiFile - midi file writer for patterns

feb 10th 2005.

takes a pattern as input and outputs a .mid file.

	*new("pathname");
	*write(pattern, maxEvents, quant);
	
the 'quant' argument defaults to 0. 
a duration of 1 equals one quarter note, calculate all values accordingly.


the input pattern can: 

	-specify pitch using \freq, \midinote, or \degree
	-inlcude rests (ie: \rest) in the sequence
	-include chords (ie: arrays in the pitch field)	

an example of how to use MidiFile:

a=Pbind(\channel,1,\midinote, Pwhite(60,80),\dur, Prand([0.5,1],inf));
b=Pbind(\channel,2,\midinote, Pwhite(30,60),\dur, Prand([0.5,1],inf));
c=Ppar([a,b]);
m=MidiFile.new("newnewnew");
m.write(c,300);

you will now see a file called newnewnew.mid in your SC3 folder. open it in whatever application you desire, although i can only guarantee that the durations will be consistent in Finale.

*/
MidiFile  {

	classvar <>tempoScale = 1024;
	var 	<>pathName;
	var 	<>scSeqs;		// format: [dur, key#, ampInDB, sustain]
	var 	<>midiSeqs;	// byte form for MIDI file with key-ups as separate events
					// events: variableLengthDur kd|channel key# vel	
	var	<>format;		// single, multi, or pattern oriented MIDI sequence
	var	<>division;	// division: number of ticks per quarter note
	
	var	<>theCmd, <>theChan, <>numbytes;
	var 	<>kdIndices;
	var 	<>curTime;
	
	*new { arg pathName; 
		^super.new.pathName_(pathName).format_(1).division_(tempoScale);
	}
	
	write { arg pattern, maxEvents = 2000, quant=0;
		var theFile, activeSeqs;
		this.collectSCSeqs(pattern, maxEvents, quant);
		this.convertSCSeqsToMIDISeqs;
		this.adjustSCSeqTimings;
		activeSeqs = midiSeqs.select({ arg seq; seq.size != 0});

		theFile = File(pathName++".mid","wb+");
		theFile.putString("MThd");			// make the MID file header
		theFile.putInt32(6);				// 6 data bytes: format, numberOfTracks, division
		theFile.putInt16(format);			
		theFile.putInt16(activeSeqs.size);	
		theFile.putInt16(division);
					
		activeSeqs.do({ arg seq;			// now write the tracks
			theFile.putString("MTrk");
			theFile.putInt32(seq.size);
			seq.do({ arg b; theFile.putInt8(b);});
		});
		theFile.close;
	}
	
	play {  
		Ppar(this.makePatternArray).play
	}
	
	makePatternArray {var seqs;
		seqs = scSeqs.select({ arg seq; seq.size != 0 });
		^seqs.collect({arg seq;
			Pbind(\tempo, 2,
				[\dur,\midinote,\db,\sustain],Pseq(seq) 
			)
		});	
	}

	collectSCSeqs 	{arg pattern, maxEvents=2000, quant=0;
		var stream, array, tempArray, holdover, time;
		time = 0;
		scSeqs = Array.fill(16, {Array.new(maxEvents)});
		Event.parentEvents.put(\channel, 1);
		stream = pattern.asStream;
		maxEvents.do({|event|
			event = stream.next(Event.default);
			event.use {var temp;
				~finish.value;
				if (~freq.value.isKindOf(Symbol).not,
					{
						if (~freq.value.isKindOf(Collection), { // an array of freqs?
							~freq.value.do({arg fr;
								if ( fr.isKindOf(Symbol).not, {
									temp = [
										time, 
										fr.cpsmidi.round(0.01), 
										~db.value, 
										~dur.value];
									holdover=0;
									scSeqs[~channel-1].add(temp);
									temp.postln;
								});
							});
						} , { // must be just one

							temp = [
								time, 
								~freq.value.cpsmidi.round(0.01), 
								~db.value, 
								~dur.value];
							holdover=0;
							scSeqs[~channel-1].add(temp);
							temp.postln;
						});
					}
				);
			};
			time = time + event.delta;
		});	
		16.do({arg chan;		// optional quantization (quant of 0 => no quant)
			scSeqs[chan].do({arg note;
				note[0]=note[0].round(quant);
			});
		});
		scSeqs;
	}
	
	adjustSCSeqTimings { 
	// convert absolute timings to incremental
		var newSeqs;
		newSeqs = Array.fill(16,0);
		scSeqs.do({ arg seq, i;
			newSeqs.put(i, seq.collect({ arg packet, i;
				if (i == (seq.size - 1), { 
					packet.put(0,0);
				}, {
					packet.put(0, seq.at(i+1).at(0) - packet.at(0));
				});
				packet;
			}));
		});
		scSeqs = newSeqs;
	}

	convertSCSeqsToMIDISeqs {
		var convertedSeqs;
		convertedSeqs = Array.fill(16,{SortedList(12, {arg a, b; a.at(0) < b.at(0)}) } );
		scSeqs.do({arg seq, i; var convert;
			convert = convertedSeqs.at(i);
			seq.do({arg evt;
				// rests have a Symbol as their second argument
				// not needed
				if (evt.at(1).isKindOf(SimpleNumber), {
					convert.add([evt.at(0), evt.at(1), (evt.at(2) + 128) max: 0 min: 127]);
					convert.add([evt.at(0) + evt.at(3), evt.at(1), 0]);
				});
			});
		});	
		midiSeqs = convertedSeqs.collect({ arg seq, i;
			var dT, time, midiSeq;
			time = 0;
			midiSeq = [];
			seq.do({arg evt; 
				// note: time accumulates in terms of integer dT's in order to
				// handle round-off error
				dT = ((evt.at(0) * tempoScale) - time).asInteger;
				time = time + dT;
				
				dT = this.convertToVLInteger(dT);
				midiSeq = midiSeq ++ dT ++ [144 + i, evt.at(1), evt.at(2)];
			});
			if(midiSeq.size != 0, { 
				midiSeq = midiSeq ++ [0, -1, 47, 0];  // add "end of track" meta-event
			});
			midiSeq;
		});
	}
	
	
	convertToVLInteger { arg dT;
		var dTArray;
		dT = dT.asInteger;
		dTArray = [dT & 127];
		dT = dT >> 7;
		while ({dT != 0;},
		{
			dTArray = [dT & 127 - 128] ++ dTArray;
			dT = dT >> 7;
		});
		^dTArray;
	}		

	getVl  { arg file; 
		var accum = 0, cur = 0;
		while ( {(cur = file.getInt8) < 0}, { accum = (accum << 7) + 128 + cur });
		^accum = (accum << 7)  + cur;
	}

	getTime { arg file; 
		curTime = curTime + this.getVl(file);
	}

	handleMeta { arg file; var id, len, str;
		id = file.getInt8;
		len = this.getVl(file);
		file.seek(len,1);
	}
	
	handleSysex { arg file; var len;
		len = this.getVl(file);
		file.seek(len,1);
	}
	
	handleMIDI { arg cmd, file;
		theCmd = (cmd + 128) >> 4;
		theChan = (cmd + 128) & 15;
		numbytes = [2,2,2,2,1,1,2].at(theCmd);
		this.handleRunningStatus(file.getInt8, file);
	}
	
	// MIDI events are transferred to on of 16 arrays corresponding
	// to the basic 16 MIDI channels.
	// Only MIDI key down and up are currently supported.
	// Key ups are matched with key downs and used to create 
	// a duration field for each midi key down event.
	// So, the format of elements of scSeqs is:
	//	[time, key, vel, duration]
	
	handleRunningStatus { arg val, file; 
		var packet;
		var kdpacket;
		var curSeq;			
	
		curSeq = scSeqs.at(theChan);
		
		packet = [curTime,  val];
		if(numbytes == 2, {packet = packet ++ [file.getInt8];});
		if (theCmd == 1, {
			if (packet.at(2) == 0, {
				kdpacket = curSeq.at(kdIndices.at(packet.at(1)));
				if( kdpacket.notNil, {
					kdpacket = kdpacket ++ [curTime - kdpacket.at(0)];
					curSeq.put(kdIndices.at(packet.at(1)), kdpacket);
				});
			}, {			
				kdIndices.put(packet.at(1),curSeq.size);
				curSeq = curSeq ++ [packet ];
			})
		});
		
		if (theCmd == 0, {
			kdpacket = curSeq.at(kdIndices.at(packet.at(1)));
			if( kdpacket.notNil, {
				kdpacket = kdpacket ++ [curTime - kdpacket.at(0)];
				curSeq.put(kdIndices.at(packet.at(1)), kdpacket);
			});
		});
		scSeqs.put(theChan, curSeq);
	}


	processChunk { arg file;
		var header, length, format, tracks, division, val, trackEnd;
		header = String[file.getChar,file.getChar,file.getChar,file.getChar];
		length = file.getInt32;
		
		curTime = 0;
		if(header == "MThd", {
			format = file.getInt16;
			tracks = file.getInt16;
			division = file.getInt16;
			// Post << header << " " << length << " " << format << " " << tracks << " " << division;
		});
		if(header == "MTrk", {
			// Post << nl << header << " " << length << nl;
			trackEnd = length + file.pos;
			while(
				{trackEnd != file.pos}, 
				{ 
				
				this.getTime(file); 
				val = file.getInt8;
				 if (val < 0, {
					if (val >= -16, {
				 		if (val == -1, { this.handleMeta(file)});
					 	if (val == -9, { this.handleSysex(file)});
						if (val == -16,{ this.handleSysex(file)});
					}, {
						this.handleMIDI(val, file);
					});
				 }, {
						this.handleRunningStatus(val, file);
				 });
			});
		});
	}
	
	read { var file, time;
		file = File(pathName,"r");
		scSeqs = Array.fill(16,{SortedList(12, {arg a, b; a.at(0) < b.at(0)}) } );
		scSeqs.do({arg seq; seq.add([0,\rest, 0, 0]) });
		kdIndices = Array.fill(128,0);
		while (
			{file.pos != file.length},
			{this.processChunk(file)}
		);
	
		// convert from absolute to incremental time values
		scSeqs.do({ arg seq;
			seq.do({arg packet, i; var dt;
				("packet:      "++i).postln;
				if (seq.at(i +1).notNil, {
					dt = seq.at(i +1).at(0) - seq.at(i).at(0);
					("dt:"++dt).postln;
				}, {
					dt = 0;
				});
				packet.put(0,dt/tempoScale);
				//packet.put(2, (packet.at(2) * 48/128) - 48); // convert velocity, assume 48dB range
				//(packet[3]/2).postln;
				//packet.put(3, packet.at(3)/tempoScale);
				("             "++packet).postln;
				"".postln;
			});
		});
		file.close;
	}
	
	patArray {var file, time, array;
		file = File(pathName,"r");
		scSeqs = Array.fill(16,{SortedList(12, {arg a, b; a.at(0) < b.at(0)}) } );
		scSeqs.do({arg seq; seq.add([0,\rest, 0, 0]) });
		kdIndices = Array.fill(128,0);
		while (
			{file.pos != file.length},
			{this.processChunk(file)}
		);
	
		// convert from absolute to incremental time values
		scSeqs.do({ arg seq;
			seq.do({arg packet, i; var dt;
				if (seq.at(i +1).notNil, {
					dt = seq.at(i +1).at(0) - seq.at(i).at(0);
				}, {
					dt = 0;
				});
				packet.put(0,dt/tempoScale);
				packet.pop;
				packet.pop;
				packet;
				});
		});
		file.close;
		scSeqs.do({arg seq; seq.removeAt(0)});
		array = scSeqs.select({ arg seq; seq.size != 0 });
		^array;
	}
	

	
}
