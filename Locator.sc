Locator
{

	// Code by C. Hutchins.  Released under GPL.

 	// this class can do vitrual location information, calculating amplitude differences
 	// and delay for a sound, to help stereoize it.  This version if it keeps the orignal
 	// amplitude in the nearest speaker and only reduces amplitude in the far speaker.
 	// That sounds a lot better, especially if the speakers are far apart.

 	// In Future versions, I want to compute echo based on the location of a virtual back wall

 	// all distances are in meters.

	classvar <fxSynthDefName;

	var <right_speaker;
	var <measurement_distance;
	var <left_front;
	var <right_front;
	var <line_of_sources;
	var <left_amp;
	var <right_amp;
	var <left_delay_time;
	var <right_delay_time;
	var <left_distsance;
	var <right_distance;



	*initClass {


		// this synth can act as a processor for a mono audio source

		fxSynthDefName = \locator;
			       /*
			SynthDef(\locator, {arg out = 0, in = 2, gate, delay_time_left, delay_time_right,
						left_amp, right_amp;

			var env, signal, delay_left, delay_right;

			signal = In.ar(in, 1);
			env = EnvGen.kr(Env.adsr(0.001, 0.001, 1, 0.001, 1, -4), gate, doneAction:2);

			delay_left = DelayC.ar(signal, 0.05, delay_time_left);
			delay_right = DelayC.ar(signal, 0.05, delay_time_right);

			Out.ar(out, delay_left * left_amp * env);
			Out.ar(out + 1, delay_right * right_amp * env);
		}).writeDefFile;*/
	}


	// the left speaker is at virtual location (0,0), the right speaker is at (rightspeaker, 0)

	// the measurement distance is the virtual distance at which the amplitude of the
	// source sound was measured.

	*new { arg right_speaker = 3.0, measurement_distance = 0.5;

		^super.new.init(right_speaker, measurement_distance);
	}


	init { arg right_speaker = 3.0, measurement_distance = 0.5;

		this.right_speaker = right_speaker;
		this.measurement_distance = measurement_distance;
	}


	// left_front and right_front are boundary cordinates for virtual sound lcoations

	measurement_distance_ {arg dist;

		measurement_distance = dist;
		left_front = [0, dist * 2];
		(right_speaker.notNil). if({
			right_front = [right_speaker, dist * 2];
		});
	}


	right_speaker_ {arg x_coordinate;

		right_speaker = x_coordinate;
		(measurement_distance.notNil). if ({
			right_front = [right_speaker, measurement_distance * 2];
		});
	}


	// this method creates number virtual source locations, in a line, distance_behind back
	// from the speakers

	make_a_line { arg number = 1, distance_behind = 2.0, amp = 1.0;

		var spacing, l;


		if (distance_behind <= measurement_distance, { // make sure we're back some
			distance_behind = measurement_distance + distance_behind;
		});

		if(number ==1, {// just one in the middle
			line_of_sources = [
					Locator.new(right_speaker, measurement_distance).compute_stereo_information(
						right_speaker / 2, distance_behind, amp)
				];
		} , {

			// figure out how faw apart they should be

			spacing = (1 / (number -1)) * right_speaker;
			line_of_sources = [];

			// then make them and initialize them based on virtual locations
			number.do({ arg count;

				l = Locator.new(right_speaker, measurement_distance);
				l.compute_stereo_information(count * spacing, distance_behind, amp);

				line_of_sources = line_of_sources.add(l);
			});
		});

		^line_of_sources;

	}


	get_amplitude { arg distance, amp;

	  	// measurement_distance is the distance from the source at which the amplitude
  		// is measured.  For instance, the amplitude might be 0.8 at 0.1 meters

  		// distance is for which we are then calculating the amplitude

  		var db_difference, new_amp;


  		// avoid divide by zeros and other loud sounds
  		// it's usually best to make sure your distance will always be greater than
  		// the measurement distance
  		if (distance < measurement_distance, {

  			^amp;
		}, {

			// difference in db = 20 * log (distance1 / distance2)
			db_difference = (measurement_distance / distance).log10 * 20;

			// do conversions from amplitude to db and back
			// and figure out the new amplitude
			new_amp = (amp.ampdb + db_difference).dbamp;

			^new_amp;
		});
	}

	compute_stereo_information { arg x_coordinate, y_coordinate, amp;

		// find information for a mono sound from one point.

	  	// the left speaker is at (0,0), the right speaker is at (right_speaker_postion, 0)
  		// the source of the sound is at (x_coordinate, y_coordinate)
		// all measurements are in meters

  		var y_coordinate_squared, closer;

		y_coordinate_squared = y_coordinate.squared;  // faster to just calculate once

		// get distance between the sound and each speaker
		// distance = ((x1 - x2)^^2 + (y1 - y2)^^2).sqrt
		left_distsance = (x_coordinate.squared + y_coordinate_squared).sqrt;
		right_distance = ((x_coordinate - right_speaker).squared + y_coordinate_squared).sqrt;

		// get the delay time between the sound and each speaker
		left_delay_time = left_distsance / 343;  // sound travels 343 m /s
		right_delay_time = right_distance / 343;

		// get the amplitude at each speaker - old method where sounds get quieter in the middle
		//left_amp = this.get_amplitude(left_distsance, amp);
		//right_amp = this.get_amplitude(right_distance, amp);

		// we're now computing the amplitude of the closer speaker based on the value of y
		closer = this.get_amplitude(y_coordinate, amp);

		// and figuring out the difference for the farther speaker

		if (left_distsance > right_distance, {
			left_amp = this.get_amplitude(left_distsance - right_distance, closer);
			right_amp = closer;
		}, {
			if (left_distsance < right_distance, {
				left_amp = closer;
				right_amp = this.get_amplitude(right_distance - left_distsance, closer);
			}, {
				// left and right are equal
				left_amp = closer;
				right_amp = closer;
			});
		});
	}

	get_stereo_information { arg x_coordinate, y_coordinate, amp;
		// returns an array useful in Pbinds

		this.compute_stereo_information(x_coordinate, y_coordinate, amp);
		// return all our values for Pbinds, etc
		^[left_distsance, left_delay_time, left_amp,
			right_distance, right_delay_time, right_amp];
	}

	get_from_point { | point, amp|

		this.compute_stereo_information(point.x, point.y, amp);
		// return all our values for Pbinds, etc
		^[left_distsance, left_delay_time, left_amp,
			right_distance, right_delay_time, right_amp];
	}



}
