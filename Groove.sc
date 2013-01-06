Groove : Quant {

	var <steps, <durs, <strong_emphasis, <weak_emphasis, bar_length, elapsed_dur;
	
	
	*new { |bar_length, beatsPerBar = 4, strong_ratio =
			1.02, weak_ratio = 1.01, quant = 0, phase, timingOffset = 0.5 |
	
		^super.new.init(bar_length, beatsPerBar,
						strong_ratio, weak_ratio, quant, phase, timingOffset);
	}
	
	*newFromClock {|clock, strong_ratio = 1.02, weak_ratio = 1.01, quant = 0, 
					phase, timingOffset |
					
		^super.new.init(clock.beatsPerBar * clock.beatDur, clock.beatsPerBar,
						strong_ratio, weak_ratio, quant, phase, timingOffset);
	}
	
	init { |bar_length, beatsPerBar = 4, strong_ratio =
			1.02, weak_ratio = 1.01, quan = 0, phas, timingOffse |
	
		quant = quan;
		phase = phase;
		timingOffset = timingOffse;
		
		this.reset(bar_length, beatsPerBar, strong_ratio, weak_ratio);
		
	}
	
	
	calcDurs { |bar_len, beatsPerBar = 4, strong_ratio = 1.02, weak_ratio = 1.01|
		
		var time, base_dur, beat_dur;
		
		bar_length = bar_len;
		
		(durs.isNil || steps.isNil || ( steps == 0 ) || (steps != beatsPerBar)).if ({
		
			this.rebeat(beatsPerBar, strong_ratio, weak_ratio);
		}, {
		
			base_dur = bar_length / ((weak_emphasis.size * weak_ratio) +
								(strong_emphasis.size * strong_ratio) +
								(steps - (weak_emphasis.size + strong_emphasis.size)));

			durs = [];
			elapsed_dur = [0];
			time = 0;
		
			steps.do ({arg index;

				strong_emphasis.includes(index).if ({
					beat_dur = base_dur * strong_ratio;
					durs = durs.add(beat_dur);
				} , {
	
					weak_emphasis.includes(index).if ({
						beat_dur = base_dur * weak_ratio;
						durs = durs.add(beat_dur);
					} , {
						beat_dur = base_dur;
						durs = durs.add(base_dur);
					})
				});
				time = time + beat_dur;
				elapsed_dur = elapsed_dur.add(time);
			});
			
		});
		
	}
	
	//if somebody really wants to do this, they can do this.durs.normalizeSum and
	// multply it by their total duration
	/*scaleDurs { | scaled_len, strong_ratio = 1.02, weak_ratio = 1.01 |
	
		var time, base_dur, beat_dur, dur_arr, elapsed;
		
		

		(durs.isNil || steps.isNil || ( steps == 0 ) ).if ({
		
			dur_arr = nil;
		} , {
		
			base_dur = scaled_len / ((weak_emphasis.size * weak_ratio) +
								(strong_emphasis.size * strong_ratio) +
								(steps - (weak_emphasis.size + strong_emphasis.size)));

			dur_arr = [];
			elapsed = [0];
			time = 0;
		
			steps.do ({arg index;

				strong_emphasis.includes(index).if ({
					beat_dur = base_dur * strong_ratio;
					dur_arr = dur_arr.add(beat_dur);
				} , {
	
					weak_emphasis.includes(index).if ({
						beat_dur = base_dur * weak_ratio;
						dur_arr = dur_arr.add(beat_dur);
					} , {
						beat_dur = base_dur;
						dur_arr = dur_arr.add(base_dur);
					})
				});
				time = time + beat_dur;
				elapsed = elapsed.add(time);
			});
		
	
			
		});
		
		dur_arr.return;
	}*/
	
		
	
	reset { |bar_len, beatsPerBar = 4, strong_ratio = 1.02, weak_ratio = 1.01|
	
		"reset".postln;
		bar_length = bar_len;
		this.rebeat(beatsPerBar, strong_ratio, weak_ratio);
	}
		
	
	beatsPerBar_ { |newBeatsPerBar = 4, strong_ratio = 1.02, weak_ratio = 1.01|
	
		this.rebeat(newBeatsPerBar, strong_ratio, weak_ratio);
	}
	
	
	rebeat { |beatsPerBar = 4, strong_ratio = 1.02, weak_ratio = 1.01|

		var base_dur, time, beat_dur, local_durs, skip;

		steps = 0;
		strong_emphasis = [];
		weak_emphasis = [];

		// measures have to be subdivided in groups of 2, 3, or 4

		{steps < (beatsPerBar + 5)}.while({

			// measures (and groups) start with a strong beat
			
			strong_emphasis = strong_emphasis.add(steps);
			
			// then they have 2 or 3 steps with no emphasis
			
			skip = [2, 3, 4, 5].choose;
			
			( skip == 2). if ({
	

				steps = steps + 2;

				// if it's two beats, then the next beat could be weak or strong
				
				//([true, false].choose).if ({
					//then add a weak emphasis
				
				//	weak_emphasis = weak_emphasis.add(steps);
				
					// and then skip two or three more beats
				//	steps = steps + [2,3].choose;
	
					// the next beat will be a strong emphasis
				//});

			}, { (skip == 3).if({
	
				
				// if we skip three beats, then no weak empahsis, 
				// the next one will be strong
				
				steps = steps + 3;
			}, { (skip == 4).if({
			
				// if we skip 4, then weak after 2 and the next will be strong
				weak_emphasis = weak_emphasis.add(steps + 2);
				steps = steps + 4;
				
			}, { // (skip == 5 )
			
				// weak after 2 or 3
				weak_emphasis = weak_emphasis.add(steps + [2, 3].choose);
				steps = steps + 5;
			}) }) })
		});

		// rather than deal with the knapsack problem, we just over-compute
		// and then trim. (This is cool because sometimes the last beat
		// of a measure is strong, which is kind of interesting)
		
		steps = beatsPerBar;
		
		//"=====\n=======\n=================\n=======================".postln;
		//"steps ".post; steps.postln;



		// I think those two are equivalent, but the bottom one makes me feel happier

		//base_dur = bar_length / ((weak_emphasis.size * (weak_ratio - 1)) +
		//						(strong_emphasis.size * (strong_ratio - 1)) +
		//						steps);
								
		base_dur = bar_length / ((weak_emphasis.size * weak_ratio) +
								(strong_emphasis.size * strong_ratio) +
								(steps - (weak_emphasis.size + strong_emphasis.size)));
		
		// in this part, we compute the durations for all the beats
		
		// weakly emphasized beats are (generally) longer than non-emphasized beats
		// and strongly emphasized beats are (generally) longer still
								
		local_durs = [];
		elapsed_dur = [0];
		time = 0;
		
		steps.do ({arg index;

			strong_emphasis.includes(index).if ({
				beat_dur = base_dur * strong_ratio;
				local_durs = local_durs.add(beat_dur);
			} , {
	
				weak_emphasis.includes(index).if ({
					beat_dur = base_dur * weak_ratio;
					local_durs = local_durs.add(beat_dur);
				} , {
					beat_dur = base_dur;
					local_durs = local_durs.add(base_dur);
				})
			});
			time = time + beat_dur;
			elapsed_dur = elapsed_dur.add(time);
		});
		
		// lose the last elapsed_dur
		//elapsed_dur.pop;	
		
		durs = local_durs.copy;
		
		durs.postln;	

	}
	
	
	isStrong { |beat|
	
		^strong_emphasis.includes(beat);
	}
	
	isWeak { |beat|

		^weak_emphasis.includes(beat);
	}
	
	
	beats { | clock|

		// this works like TempoClock.beats, to return a floating point number indicating
		// where we are between beats
		//
		// there will obviously be a lag, given that this isn't calling a primative 
		// and is doing a search operation
		//
		// in an attempt to reduce overhead, avoid calling this method from within the
		// class, but just copy the code

		var next_clock_beat, time_in_measure, current_beat, time_at_start_of_measure,
			groovy_beat, current_time;
		
		// the clock can tell us our current time and current beat (according to it's
		// fixed duration concept of beats)
		// from this we can calculate what time the measure started,
		// the current time in the measure,
		// and our current beat
		//
		// we can use this information to find out when our next beat will happen
		// and the duration of our next beat, which is what subsequent methods do
		
		current_beat = clock.beatInBar;
		//current_time = clock.seconds;
		//next_clock_beat = current_beat.roundUp(1);
		time_in_measure  = current_beat * clock.beatDur;
		//time_at_start_of_measure = current_time -  time_in_measure;
		groovy_beat = elapsed_dur.indexInBetween(time_in_measure);

		^groovy_beat;
	}

	
	nextTimeOnGrid { | clock |
	
		var next_clock_beat, time_in_measure, current_beat, time_at_start_of_measure,
			groovy_beat, current_time, local_phase, nextTime, push, subBeatDur;
		
		current_beat = clock.beatInBar;
		//current_time = clock.beats2secs(clock.beats);
		subBeatDur = clock.beatDur / clock.beatsPerBar;
		//next_clock_beat = current_beat.roundUp(1);
		time_in_measure  = clock.beatInBar * clock.beatDur;
		time_at_start_of_measure = clock.beats - clock.beatInBar;//current_time -  time_in_measure;
			//clock.beats2secs(clock.bars2beats(clock.bar));
		"beat in bar ".post; clock.beatInBar.postln;
		"beat dur ".post; clock.beatDur.postln;
		//"cuurent time ".post; current_time.postln;
		"start measure ".post; time_at_start_of_measure.postln;
		"in measure ".post; time_in_measure.postln;
		//clock.beats2secs(clock.beats).asFloat.postln;
		//groovy_beat = elapsed_dur.indexInBetween((current_time  - time_at_start_of_measure) + 
		//					(timingOffset ? 0 ));
		groovy_beat = elapsed_dur.indexInBetween((clock.beats % bar_length)
										/*(time_in_measure % bar_length)*/
										 + (timingOffset ? 0));
		//clock.beats2secs(clock.beats).postln;
		groovy_beat = groovy_beat.ceil;
		
		phase.notNil.if({ groovy_beat = groovy_beat + phase});
		
		//elapsed_dur.postln;
		//groovy_beat.postln;
		
		if ( groovy_beat > steps, {
		
			// ok, we want the first beat of the next measure
			nextTime = time_at_start_of_measure + bar_length;
		} , {
		
			nextTime = time_at_start_of_measure + 
				elapsed_dur.asArray.blendAt(groovy_beat);
		});
			
		push = 1;
			
		{nextTime < /*clock.beats2secs(clock.beats)*/clock.beats}.while ({
		
			nextTime = time_at_start_of_measure + 
				elapsed_dur.asArray.blendAt(groovy_beat + push);
			push = push+ 1;
			((push + groovy_beat) > steps).if ({
				time_at_start_of_measure = time_at_start_of_measure + bar_length;
				push = groovy_beat;
				("next measure" + nextTime).postln
			});
			"loopdeloop".postln;
		});
		
		
		"nextTime ".post; nextTime.postln;
		"time ".post; clock.beats2secs(clock.beats).postln;
		"beat ".post; groovy_beat.postln;
		
		^(nextTime);
		
	
		//^clock.nextTimeOnGrid(quant, (phase ? 0) - (timingOffset ? 0));
	}
	
	
	nextDur { |clock|
	
		var next_clock_beat, time_in_measure, current_beat, time_at_start_of_measure,
			groovy_beat, current_time;
		
		current_beat = clock.beatInBar;
		//current_time = clock.seconds;
		//next_clock_beat = current_beat.roundUp(1);
		time_in_measure  = current_beat * clock.beatDur;
		//time_at_start_of_measure = current_time -  time_in_measure;
		groovy_beat = elapsed_dur.indexInBetween(time_in_measure);

		^durs[(groovy_beat.ceil % steps)];
	}
	
	
	nextStrength { | clock|
	
		// return 2 for strong, 1 for weak, 0 for none
		
	
		var next_clock_beat, time_in_measure, current_beat, time_at_start_of_measure,
			groovy_beat, current_time, ret_value;
		
		current_beat = clock.beatInBar;
		//current_time = clock.seconds;
		//next_clock_beat = current_beat.roundUp(1);
		time_in_measure  = current_beat * clock.beatDur;
		//time_at_start_of_measure = current_time -  time_in_measure;
		groovy_beat = elapsed_dur.indexInBetween(time_in_measure);
		
		groovy_beat = groovy_beat.ceil % steps;

		strong_emphasis.includes(groovy_beat).if ({
			ret_value = 2;
		} , { weak_emphasis.includes(groovy_beat).if ({
			ret_value = 1;
		}, {
			ret_value = 0;
		})});
		
		^ret_value;
	}
	
	
	emphasis_durs{
	
		var time, result;
		
		time = 0;
		result = [];
		//"foo".postln;
		
		durs.do({ |dur, index|
			//"loop".postln;
		
			if (((strong_emphasis.includes(index) || weak_emphasis.includes(index))), {
				(time != 0). if({
					result = result.add(time);
				});
				time = dur;
				//"got one".postln;
				
			} , {
			
				time = time +dur;
				//("weak" + time).postln;
			});
		});
		
		result = result.add(time);
		
		
		^result;
	}


	strong_emphasis_durs{
	
		var time, result;
		
		time = 0;
		result = [];
		
		durs.do({ |dur, index|
		
			//("loop" + index).postln;
			(strong_emphasis.includes(index)). if({
				(time != 0).if ({
					result = result.add(time);
				});
				time = dur;
				//("gotone" + index).postln;
				
			} , {
			
				time = time +dur;
			});
		});
		
		result = result.add(time);
		
		
		^result;
	}

	weak_emphasis_durs{
	
		var time, result;
		
		time = 0;
		result = [];
		
		durs.do({ |dur, index|
		
			//("loop" + index).postln;
			(weak_emphasis.includes(index)). if({
				(time != 0).if ({
					result = result.add(time);
				});
				time = dur;
				//("gotone" + index).postln;
				
			} , {
			
				time = time +dur;
			});
		});
		
		result = result.add(time);
		
		
		^result;
	}
	
	gen_amp_arr { |downbeat, strong, weak, plain|
	
		var result;
		
		result = [];
		
		durs.do({ | dur, index|
		
			(index == 0).if ({
			
				result = [downbeat];
			} , {
			
				(strong_emphasis.includes(index)). if({
				
					result = result ++ strong;
					
				} , { (weak_emphasis.includes(index)).if ({
				
					result = result ++ weak;
				}, {
					result = result ++ plain;
				})})
			})
		});
		
		^result;
	}

	
	// The quanitzation in this class tracks measures, however, a loop will usually
	// be made up of multiple measures.
	// In some music, people like to skip the last beat of a phrase, so a loop of 4
	// 4-beat measures would actually have 15 beats.
	//
	// Therefore, I've provided a push and pop class, so you can temporarily lose the
	// last beat and then glue it back on later.
	
	pop {
	
		steps = steps - 1;
		elapsed_dur.pop;
		^durs.pop;
	}
	
	push { |dur|
	
		// this dur ought to be greater than the frame size, I think.
		// I will only check if it's greater than 0, so if you put in
		// something too small, you have nobody to blame but yourself.
	
		var elapsed;
		
		if ( dur > 0, {
			durs = durs.add(dur);
			elapsed = elapsed_dur.last;
			elapsed_dur = elapsed_dur.add(elapsed + dur);
			steps = steps + 1;
		});
	}

}