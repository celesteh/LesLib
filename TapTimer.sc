TapTimer {

	// Code by C. Hutchins.  Released under GPL.
	
	// Tap on a button, a keyboard, a MIDI device, or whatever to get timings
	
	// This class requires the BBCut library (change the ExternalClock to a TempoClock
	// if you don't want / have BBCut
	
	//To do keyboard triggering, you use the Document class. For example:
	//
	// (
	//	var doc, timer;
	//
	//	timer = TapTimer.new(32);
	//	doc = Document.new;
	//	doc.keyDownAction_({arg thisDoc, key;
  	//		var time;
  	//		if((key == $t), {
     //			time = Main.elapsedTime;
     //	 		timer.tap(time);
  	//		});
	//	});
	// )
	//
	// Then, when you want something to happen according to the clock, you wrap it 
	// in a routine. From within the same doc.keyDownAction:
	//
    	//	if((key == $a) , {
   	//		Routine.new({Synth(\example).play; }).play(timer.tempoclock);
    	//	}, { if ((key == $b), {
   	//		Routine.new({Pbind.play}).play(timer.tempoclock);
   	//	}) });
	//
	
	//classvar <>mIN_LEN;
	
	var clock, last_time, <phrase_len, <tempo, <beats_per_phrase, <>mIN_LEN, 
		mAX_LEN, timearr, <>error_margin, pr_beat_flag;
	
	
	//*initClass {
	
	//	mIN_LEN = 0.000001;
	//}
	
	*new { arg max = 16, phrase_len = 4, beats_per_phrase = 4, error_margin = 0.1, 
			initClock = true, mIN_LEN = 0.000001;
	
		^super.new.init(max, phrase_len, beats_per_phrase, error_margin, initClock, mIN_LEN);
	}
	
	
	init { arg max = 16, len = 4, beats = 4, error = 0.05, initClock = true, min =  0.000001;
	
		var t_clock;
		
		mAX_LEN = max;
		mIN_LEN = min;
		phrase_len = len;
		beats_per_phrase = beats;
		last_time = 0;
		//tempo = phrase_len / beats_per_phrase;
		// beat / time
		tempo = beats_per_phrase / phrase_len;
		(initClock).if ({
		
			//how we did it in SC 3.2:
			t_clock = TempoClock(tempo, beats_per_phrase);
			clock = ExternalClock(t_clock);
			
			//externalclock = TempoClock(tempo, beats_per_phrase);
			clock.play;
			//t_clock.schedAbs(t_clock.nextBar, {t_clock.beatsPerBar_(beats_per_phrase)});
			
			});
		timearr = [];
		error_margin = error;
		pr_beat_flag = false;
	}
	
	tempoclock {
	
		(clock.notNil). if ({
			^clock.tempoclock;
			//^externalclock
		} , {
			//calls the method below to make an external clock
			// SC 3.2 
			^this.clock.tempoclock;
			//^nil;
			//externalclock = TempoClock(tempo, beats_per_phrase);
			//externalclock.play;
			//^externalclock

		});
	}
	
	clock {
	
		//SC 3.2 code
		var t_clock;
	
		(clock.notNil). if ({
			^clock;
		} , {
			// if we don't have it, make it
			this.tap;
			(clock.notNil). if ({
				^clock;
			} , {
				t_clock = TempoClock(tempo, beats_per_phrase);
				clock = ExternalClock(t_clock);
				clock.play; 
				//t_clock.schedAbs(t_clock.nextBar, {t_clock.beatsPerBar_(beats_per_phrase)});
				^clock;
			});				
		});
		
		//^this.tempoclock
	}
	
	isTicking {
	
		^clock.notNil;
	}
	
	beats_per_phrase_ { arg beats;
	
		var t_clock;
		
		beats_per_phrase = beats;
		//tempo = phrase_len / beats_per_phrase;
		// beat / time
		tempo = beats_per_phrase / phrase_len;
		//externalclock = ExternalClock(TempoClock(tempo)).play;
		clock.notNil.if ({
		
			t_clock = clock.tempoclock;
			t_clock.tempo = tempo;
			
			pr_beat_flag.not.if ({
			
				pr_beat_flag = true;
				t_clock.schedAbs(t_clock.nextBar, {
					t_clock.beatsPerBar_(this.beats_per_phrase);
					"changed beats".postln;
					pr_beat_flag = false;
				});
			});
		});
	}
	
	
	start_tap { arg time;
		// this tap doesn't count if the clock is already going
		(clock.notNil.not).if ({
			this.tap(time);
		} , {
			(last_time == 0). if ({
				(time.isNil). if ({
					time = Main.elapsedTime;
				});
				last_time = time;
				"first tap".postln;
			});
		});
	}
	
	
	tap 	{ arg time;
			var current, avg, fudge, t_clock, beats, return_val;
	
		((time.notNil).not). if ({
			time = Main.elapsedTime;
		});
		
		(last_time == 0). if ({
			last_time = time;
		} , {
			current = time - last_time;

			((current <= mAX_LEN) && (current >= mIN_LEN)) .if ({

				avg = timearr.sum / timearr.size;
				fudge = error_margin * current;
				
				// is the tap part of an existing series of taps?
				// or is it a new time?
				
				(( avg < ( current + fudge)) &&
					( avg > ( current - fudge))). if ({
						// this tap was part of a series
						// do some averaging
						timearr = timearr.add(current);
						phrase_len = (timearr.sum / timearr.size);// * beats_per_phrase;
						//tempo = phrase_len / beats_per_phrase;
						// beat / time
						tempo = beats_per_phrase / phrase_len;
						//externalclock = ExternalClock(TempoClock(tempo)).play;
						
						// we want the tempo to give us notes between 0.09 and 0.125 in duration
						//beats = beats_per_phrase;
						//{( tempo < 5.56)}.while ({
						//	beats_per_phrase = beats_per_phrase * 2;
						//	tempo = beats_per_phrase / phrase_len;
						//});
						//{ (tempo > 11.12) && (beats_per_phrase > 1)}.while ({
						//	beats_per_phrase = beats_per_phrase - 1;
						//	tempo = beats_per_phrase / phrase_len;
						//});
						
						
						clock.notNil.if ({
		
							t_clock = clock.tempoclock;
							//t_clock.tempo = tempo;
							t_clock.schedAbs(t_clock.nextBar,
									 {
									 	t_clock.tempo = tempo;
									 	t_clock.beatsPerBar_(beats_per_phrase)});
						}, { // start the clock
							// SC 3.2 
							t_clock = TempoClock(tempo, beats_per_phrase);
							t_clock.play;
							clock = ExternalClock(t_clock).play; 
							
							//externalclock = TempoClock(tempo, beats_per_phrase);
							clock.play;

							//t_clock.schedAbs(t_clock.nextBar, 
							//		{t_clock.beatsPerBar_(beats_per_phrase)});
						});
				} , {
				
					// we are establishing a new time
					
					phrase_len = current  * beats_per_phrase;
					//tempo = phrase_len / beats_per_phrase;
					// beat / time
					tempo = beats_per_phrase / phrase_len;
					//externalclock = ExternalClock(TempoClock(tempo)).play;
					clock.notNil.if ({
		
						t_clock = clock.tempoclock;
						t_clock.tempo = tempo;
						t_clock.schedAbs(t_clock.nextBar, 
								{t_clock.beatsPerBar_(beats_per_phrase)});
					}, { // start the clock
							// SC 3.2 
							t_clock = TempoClock(tempo, beats_per_phrase);
							clock = ExternalClock(t_clock).play; 
							
							//externalclock = TempoClock(tempo, beats_per_phrase);
							clock.play;
						//t_clock.schedAbs(t_clock.nextBar, 
						//		{t_clock.beatsPerBar_(beats_per_phrase)});
					});
			 		timearr = [current];
			 	});
			 	
			 	return_val = true; // we have recorded the beat
			 } , {
			 	return_val = false; // we have not recorded the beat
			 });	
			 last_time = time;
		});
		phrase_len.postln;
		
		clock.play;
		
		^return_val;
	}

	//change by powers of 2	
	double {
	
		var new_len;
		
		new_len = phrase_len * 2;
		
		(new_len <= mAX_LEN).if ({
			phrase_len = new_len;
			beats_per_phrase = beats_per_phrase * 2;
		});
	
	}
	
	half {
		var new_len;
		
		new_len = phrase_len / 2;
		
		(new_len >= mIN_LEN). if ({
			phrase_len = new_len;
			beats_per_phrase = beats_per_phrase /2;
		});
	
	}
	
	quad {
		var new_len;
		
		new_len = phrase_len * 4;
		
		(new_len <= mAX_LEN).if ({
			phrase_len = new_len;
			beats_per_phrase = beats_per_phrase * 4;
		});
	
	}
	
	eight {
		var new_len;
		
		new_len = phrase_len * 8;
		
		(new_len <= mAX_LEN).if ({
			phrase_len = new_len;
			beats_per_phrase = beats_per_phrase * 8;
		});
	
	}
}
				
				
	
	
	