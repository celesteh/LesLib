SingleSample {

	// todo: save the window in a var, so as to not create abillion of them

	var <buf, max_right, <>max_len, <conductor, <>pattern, <name, <>shiftarr, durarr, locator,
		micro_grains, <>bbcut, evt, <>continious, events, flag, remover;

	*alloc { arg server, name, numChannels = 1, max_len = 16, max_right = 3,
					show=true, num_grains = 1, conductor, action;

		^super.new.init_alloc(server, name, numChannels, max_len, max_right, show, num_grains,
							conductor, action);
	}

	*new { arg name, buf, max_len = 16, max_right = 3, show=true, conductor;

		^super.new.init(name, buf, max_len, max_right, show, conductor);
	}

	*grains  { arg name, buf, max_len = 16, max_right = 3, show=false, num_grains = 1, conductor;

		^super.new.init_grains(name, buf, max_len, max_right, show, num_grains, conductor);

	}

	*open { arg server, name, path, max_len = 16, max_right = 3,
					show=true, num_grains = 1, conductor, action;

		^super.new.init_open(server, name, path, max_len, max_right,
						show, num_grains, conductor, action);
	}

	*loadDialog {arg server, name,  max_len = 16, max_right = 3,
					show=true, num_grains = 1, conductor, action;

		^super.new.init_dialog(server, name, max_len, max_right,
						show, num_grains, conductor, action);
	}

	*mono { arg server, name, path, max_len = 16, max_right = 3,
					show=true, num_grains = 1, conductor, action;

		^super.new.init_alloc(server, name, 1, max_len, max_right, show, num_grains,
							conductor, action:{ |ss|

				ss.append(path, 0, -1, 1, action);
			});
	}


	init_open {arg srv, name, path, max_len = 16, max_right = 3,
				show=true, num_grains = 1, conductor, action;

		//var mybuf;

		path.postln;

		buf = BufferTool.read(srv, path, class:Buffer, action:{

			this.init_grains(name, buf, max_len, max_right, show, num_grains);
			action.value(this);
		});

		//buf = mybuf;
	}


	init_dialog {arg srv, name, max_len = 16, max_right = 3,
				show=true, num_grains = 1, conductor, action;

		var mybuf;

		mybuf = Buffer.loadDialog(srv, action: {|b|
			"init_dialog action".postln;
			buf = BufferTool.grain(srv, b);
			this.init_grains(name, buf, max_len, max_right, show, num_grains);
			action.value(this);
		});
	}


	init_alloc {arg srv, name, numChannels = 1, max_len = 16, max_right = 3,
				show=true, num_grains = 1, action;

		//var mybuf;

		buf = BufferTool.alloc(srv, /*44100 * */ max_len, numChannels, class:BBCutBuffer,
			action: {|b|

				this.init_grains(name, b, max_len, max_right, show, num_grains);
				buf.numFrames = b.numFrames;
				action.notNil.if({
					action.value(this);
				});
		});
		//buf = mybuf;
	}

	init_grains {arg name, buf, max_len = 16, max_right = 3, show=false, num_grains = 1;

		var size, grain_num, grain_size, task;

		this.init(name, buf, max_len, max_right, false);

		(buf.grains.notNil).if ({
			size = buf.grains.size;
		} , {
			(num_grains ==1). if({
				size = 1;
			}, {

				grain_size = (max_len / num_grains) * buf.sampleRate;
				buf.calc_grains_size(grain_size);
				size = num_grains
			});
		});

		grain_num = CV.new.sp(0, 0, size - 1, 1, 'linear');
		conductor.put(\grain_num, grain_num);
		this.conductor.gui.keys = this.conductor.gui.keys ++ \grain_num;
		task = Task({
			"show".postln;
			conductor.show;
		});

		if (show, {AppClock.play(task )});


	}

	init {arg na, bu, len, right, show=false, conductor;

		max_len = len;
		max_right = right;
		name = na;
		buf = bu;

		//locator = Locator.new(max_right);

		shiftarr = [64/45, 64/63, 16/9, 10/7, 12/7, 8/7, 6/5, 8/5, 4/3, 1,
					3/2, 5/4, 5/3, 7/4, 7/6, 7/5, 9/8, 63/32, 45/32];

		durarr = [1/8, 1/4, 1/2, 3/4, 1, 3/2, 2, 4];


		this.conductor_(conductor, name);


		//conductor.useInterpolator;

		if (show, {conductor.show});
		continious = true;

	}

	conductor_ { arg cond, na;

		var cv;

		(cond.isNil).if({
			conductor = Conductor.make({ arg thisc, shift, ratio, amp, dur_index, pan, mask,
									rate;
								//, xlocation, ylocation;



				shift.sp(6, 0, shiftarr.size - 1, 1, 'linear');
				shift.input_(0.5);
				ratio.spec_(\unipolar);
				ratio.value_(0.5);
				amp.spec_(\amp);
				amp.value_(0.15);
				pan.spec_(\pan);
				//xlocation.sp(max_right / 2, 0, max_right, 0, 'linear');
				//ylocation.sp(max_right / 2, 1, max_right, 0, 'linear');
				dur_index.sp(4, 0, durarr.size - 1, 1, 'linear');
				mask.sp(23, 0, 23, 1, 'linear');

				rate.sp(1, 0.1, 10, 0, 'exponential');

				thisc.name_(na);
				//this.guiItems_([shift, ratio, amp, pan, location]);
				thisc.synth_( (), [freq: 440, out: 3] );

				thisc.useInterpolator;
				//thisc.interpKeys_( [shift, ratio]);
				//thisc.presetKeys_( [shift, ratio]);
				thisc[\preset].interpItems_([shift, ratio]);

			});
		} , {

			cond.isKindOf(AbstractFunction).if ({

				conductor = Conductor.make(cond);
			} , {
				conductor = cond;
			})
		});
		/*
		conductor.valueKeys.do({ |key|
			this.addUniqueMethod(key,
				{ ^conductor.at(key )
			})
		});
		*/
		conductor.valueKeys.do({|key|
			cv = conductor[key];
			cv.action_({ |ceevee|
				continious.if({
					evt.notNil.if({
						pattern.isPlaying.if({
							evt[\server].notNil.if({
								evt.set(key, ceevee.value);
							}, {
								"server is mil".postln;
									//evt = nil;
							});
							//ceevee.value.postln;
							events.do({|e, i|
								e.notNil.if({
									e.isPlaying.if({
										e[\server].notNil.if({
											e.set(key, ceevee.value);
											}, {
												events.remove(e);
										});
										}, {
											events.remove(e);
									});
									}, {
										events.remove(e);
								});
							});
						});
					});
				});
			});
		});

		cv = conductor[\amp];
		cv.notNil.if({
			cv.action = {|ceevee|
				bbcut.notNil.if({
					bbcut.amp = ceevee.value;
				});
				continious.if({
					evt.notNil.if({
						pattern.isPlaying.if({
							evt[\server].notNil.if({
								evt.set(\amp, ceevee.value);
							}, {
								"server is mil".postln;
							});
							//ceevee.value.postln;
							events.do({|e, i|
								e.notNil.if({
									e.isPlaying.if({
										e[\server].notNil.if({
											e.set(\amp, ceevee.value);
											}, {
												events.remove(e);
										});
										}, {
											events.remove(e);
									});
									}, {
										events.remove(e);
								});
							});
						});
					});
				});
			};
		});
	}


	addCV {|key, spec, init|

		var cv;

		cv = conductor.at(key);

		cv.isNil.if({
			cv = conductor.addCV(key);
		});

		spec.notNil.if({
			init.notNil.if({
				cv.spec_(spec, init);
			}, {
				cv.spec_(spec);
			})
		});

		cv.action_({ |ceevee|
			//continious.postln;
			continious.if({
				evt.notNil.if({
					//pbind.isPlaying.postln;
					pattern.isPlaying.if({
						evt[\server].notNil.if({
							evt.set(key, ceevee.value);
						});
						//ceevee.value.postln;
					});
				});
			});
		});
		^cv;
	}

	clone { |other|
		other.conductor.valueKeys.do({|key|
			this.addCV(key, other.conductor.at(key).spec, other.conductor.at(key).value)
		});
	}


	at { |key|

		^conductor.at(key)
	}

	read {|path, fileStartFrame = 0, numFrames = -1, bufStartFrame = 0, action|

		"read!".postln;

		buf.read(path, fileStartFrame, numFrames, bufStartFrame, action:action);
	}


	readChannel {|path, fileStartFrame = 0, numFrames = -1, bufStartFrame = 0, channels, action|

		"read!".postln;

		buf.readChannel(path, fileStartFrame, numFrames, bufStartFrame, channels, action);
	}


	append { |path, fileStartFrame, nFrames, channels, action|

		buf.append(path, fileStartFrame, nFrames, channels, action);
	}


	granulate { |num|

		var size, grain_size, task, cv;

		grain_size = (max_len / num) * buf.sampleRate;
		buf.calc_grains_size(grain_size);
		size = num;

		conductor.includesKey(\grain_num).if ({
			cv = conductor[\grain_num];
			cv.sp(cv.value, 0, size - 1, 1, 'linear');
		});
	}

	words {
		var cv;

		buf.prepareWords(action: {
			conductor.includesKey(\grain_num).if ({
				cv = conductor[\grain_num];
				cv.sp(cv.value, 0, buf.grains.size - 1, 1, 'linear');
			});
		});
	}


	// respond as though declared functions were native methods to this object
	doesNotUnderstand { arg selector ... args;
		var cv;
		//"doesNotUnderstand".postln;
		//^this.call(selector,*args)
		cv = conductor.at(selector.asSymbol);
		cv.value.postln;
		^cv;
	}


	/*
	shift {

		//^shiftarr.wrapAt(conductor[\shift].value);
		^conductor[\shift];
	}

	shift_val {

		^shiftarr.wrapAt(conductor[\shift].value);
	}

	ratio {

		^conductor[\ratio];
	}

	ratio_val {

		^conductor[\ratio].value;
	}
	*/
	rate {
		(conductor.keys.includes(\rate)).if({
			^conductor[\rate];
		} , {
			^super.rate;
		})
	}
	/*
	rate_val {

		^conductor[\rate].value;
	}
	amp {

		^conductor[\amp];
	}

	amp_val {

		^conductor[\amp].value;
	}

	pan {

		^conductor[\pan];
	}

	pan_val {

		^conductor[\pan].value;
	}

	mask {

		^conductor[\mask];
	}

	mask_val {

		^conductor[\mask].value;
	}
	*/
	/*
	xlocation {

		^conductor[\xlocation];
	}

	xlocation_val {

		^conductor[\xlocation].value;
	}

	ylocation {

		^conductor[\ylocation];
	}

	ylocation_val {

		^conductor[\ylocation].value;
	}
	*/

	dur {
		//var dur;

		//dur = durarr.wrapAt(conductor[\dur].value);
		//(dur < buf.dur). if ({
		//	dur = buf.dur;
		//});
		^buf.dur;
	}

	bufnum {

		^buf.bufnum
	}

	dur_val {
		var val;

		conductor.includesKey(\dur_index).if({
			val = durarr.wrapAt(conductor[\dur_index].value);
		}, {
			//Error("Associated conductor does not include dur").throw;
			val = 1;
			("Associated conductor does not include dur. Using Bufer dur").warn;
			//val = buf.dur;
		});
		^val;
	}

	grain_num {

		//^conductor[\grain_num];
		^this.grain_num_val;
	}

	grain_num_val {

		var num;

		num = conductor[\grain_num];

		if (num.notNil, { num = num.value; });

		^num;
	}

	grain {

		var num, grain;

		num = conductor[\grain_num];

		if (num.notNil, {
			 num = num.value;
			 grain = buf.grains[num];
		}, {grain = nil });

		^grain;
	}


	isPlaying {

		^pattern.notNil;
	}

	show_status {

		var text, stat;

		(this.isPlaying) .if ({
			text = "play";
		} , { text = "stop";
		});

		stat = [name, text, "shift", this.shift_val, "ratio", this.ratio_val, "amp",
				this.amp_val, "dur", this.dur_val, "pan", this.pan_val,
				/*"location", this.xlocation_val, this.ylocation_val*/];
		stat.postln;
		^stat;
	}

	/*
	pbind { arg instrument, timer, multiple = 1, group ... pairs;
		^this.pattern(Pbind, instrument, timer, multiple = 1, group, pairs);
	}
	*/

	pbind { arg instrument, timer, multiple = 1, group ... pairs;

		var list, dur;
		list = [];

		flag = true;

		"in pbind".postln;

		this.stop_pbind;
		pattern = 1;  // it' not nil, ok?

		conductor.keys.do({|k|
			conductor[k].isKindOf(CV).if ({
				//k.asSymbol.postln;
				list = list ++ k.asSymbol ++ conductor[k]
			}) });

		("flat" + list.flatten).postln;

		group.notNil.if({
			list = list ++
				\group ++ group.nodeID
		//}, {
		//	list = list ++
		//	\target ++ Pfunc({Server.default})
		});

		"grouped".postln;

		this.dur_val.notNil.if({
			timer.notNil.if({
				(multiple != 0).if ({
					list = list ++ \dur ++ Pfunc({timer.phrase_len * this.dur_val * multiple});
				});
			})
		});

		"durval".postln;

		list.includes(\dur).not.if({
			pairs.notNil.if({
				pairs.flat.includes(\dur).not.if({
					list = list++ \dur ++ this.dur;
					"no dur".postln;
				})
			})
		});

		"dur".postln;

		list.includes(\ratio).if ({

			list = list ++
				[[\signal_amp, \shift_amp]] ++ Pfunc({arg evt;
										var rat;
										rat = evt.at(\ratio);
										evt.postln;
										[rat, 1 - rat]})
		});

		list.includes(\shift).if ({
			list = list ++
			\shift_val ++ Pfunc({arg evt;
				shiftarr.wrapAt(evt.at(\shift));
			})
		});

		"ratio".postln;


		(locator.notNil &&
			conductor[\xlocation_val].notNil &&
			conductor[\ylocation_val].notNil).if({

			list = list ++
				[[\left_distsance, \left_delay_time, \left_amp,
					\right_distance, \right_delay_time, \right_amp]] ++
									Pfunc({ arg evt;
										locator.get_stereo_information(
												this.xlocation_val, this.ylocation_val,
												evt.at(\amp));
									})

		});



		instrument.notNil.if({
			list = list ++ \instrument ++ instrument.asSymbol;
		});

		list.includes(\startFrame).not.if({
			pairs.includes(\startFrame).not.if({
				list = list ++ \startFrame ++ 	Pfunc({buf.startFrame})
			})
		});

		list = list ++
			\bufnum ++		buf.bufnum ++
			\isPlaying ++		Pfunc({|evt| evt.postln; true})// ++
			//\flag ++			Pfunc({ flag })
		;

		pairs.notNil.if({ list = list++pairs; "pairs ".post; pairs.flatten.postln; });

		list = list++ \evt ++ Pfunc({|event|
			var dur;
			evt = event;
			dur = evt.at(\dur);
			dur.notNil.if({
				//(dur > 0.5).if({
					this.pr_addEvent(event)
				//});// don't add if dur is known and is less than 0.5
				//}, {
				//	this.pr_addEvent(event)
			//});
			});
		});

		("list" + list).postln;

		timer.notNil.if({
			pattern = Ptpar([timer.tempo, Pbind.new(*list)]);

			pattern = pattern.playExt(timer.clock);
			//Routine({pbind = pbind.play}).play(timer.tempoclock);
		} , {
			pattern = Pbind.new(*list).play;
		});
		pattern.isNil.if({("wtf").postln;});
		//pbind.trace.play;
		^pattern
	}


	make_pattern {arg patt = Pbind, instrument, timer, multiple = 1, group, odditem ... pairs;

		var list, player;

		list = this.eventPairs(instrument, timer, multiple = 1, group, *pairs);
		odditem  = odditem ?? [\1, 1];

		this.stop;
		patt.post;
		( ([odditem].flat ++ list)).postln;
		timer.notNil.if({
			//pbind = Ptpar([timer.tempo, patt.new(*((odditem ++ list).flat))]);
			player = patt.new( * (([odditem].flat ++ list)));

			player = player.playExt(timer.clock);
			//Routine({pbind = pbind.play}).play(timer.tempoclock);
		} , {
				player = patt.new( * (([odditem].flat ++ list))).play;
		});

		pattern = player;
		^pattern
	}


	pmono { arg instrument, timer, multiple = 1, group ... pairs;

		var list;

		list = this.eventPairs(instrument, timer, multiple = 1, group, *pairs);

		timer.notNil.if({
			pattern = Ptpar([timer.tempo, Pmono.new(instrument, *list)]);

			pattern = pattern.playExt(timer.clock);
			//Routine({pbind = pbind.play}).play(timer.tempoclock);
		} , {
			pattern = Pmono.new(instrument, *list).play;
		});

		^pattern
	}


	eventPairs { arg instrument, timer, multiple = 1, group ... pairs;

		var list, dur;
		list = [];

		flag = true;


		conductor.keys.do({|k|
			conductor[k].isKindOf(CV).if ({
				k.asSymbol.postln;
				list = list ++ k.asSymbol ++ conductor[k]
			}) });

		("flat" + list.flatten).postln;

		group.notNil.if({
			list = list ++
				\group ++ group.nodeID
		//}, {
		//	list = list ++
		//	\target ++ Pfunc({Server.default})
		});

		"grouped".postln;

		this.dur_val.notNil.if({
			timer.notNil.if({
				(multiple != 0).if ({
					list = list ++ \dur ++ Pfunc({timer.phrase_len * this.dur_val * multiple});
				});
			})
		});

		"durval".postln;

		list.includes(\dur).not.if({
			pairs.notNil.if({
				pairs.flat.includes(\dur).not.if({
					list = list++ \dur ++ this.dur;
					"no dur".postln;
				})
			})
		});

		"dur".postln;

		list.includes(\ratio).if ({

			list = list ++
				[[\signal_amp, \shift_amp]] ++ Pfunc({arg evt;
										var rat;
										rat = evt.at(\ratio);
										evt.postln;
										[rat, 1 - rat]})
		});

		list.includes(\shift).if ({
			list = list ++
			\shift_val ++ Pfunc({arg evt;
				shiftarr.wrapAt(evt.at(\shift));
			})
		});

		"ratio".postln;


		(locator.notNil &&
			conductor[\xlocation_val].notNil &&
			conductor[\ylocation_val].notNil).if({

			list = list ++
				[[\left_distsance, \left_delay_time, \left_amp,
					\right_distance, \right_delay_time, \right_amp]] ++
									Pfunc({ arg evt;
										locator.get_stereo_information(
												this.xlocation_val, this.ylocation_val,
												evt.at(\amp));
									})

		});



		instrument.notNil.if({
			list = list ++ \instrument ++ instrument.asSymbol;
		});

		list.includes(\startFrame).not.if({
			pairs.includes(\startFrame).not.if({
				list = list ++ \startFrame ++ 	Pfunc({buf.startFrame})
			})
		});

		list = list ++
			\bufnum ++		buf.bufnum ++
			\isPlaying ++		Pfunc({|evt| evt.postln; true})// ++
			//\flag ++			Pfunc({ flag })
		;

		pairs.notNil.if({ list = list++pairs; "pairs ".post; pairs.flatten.postln; });

		list = list++ \evt ++ Pfunc({|event| evt = event; this.pr_addEvent(event);});

		//("list" + list).postln;

		^list;



		/*
		var list, dur;

		flag = true;

		list = [];

		conductor.keys.do({|k|
			conductor[k].isKindOf(CV).if ({
				list = list ++ k.asSymbol ++ conductor[k]
			}) });

		list.flatten.postln;

		group.notNil.if({
			list = list ++
				\group ++ group.nodeID
		});


		this.dur_val.notNil.if({
			timer.notNil.if({
				list = list ++ \dur ++ Pfunc({timer.phrase_len * this.dur_val * multiple});
			})
		});


		list.includes(\dur).not.if({
			pairs.flat.includes(\dur).not.if({
				list = list++ \dur ++ this.dur;
				"no dur".postln;
			})
		});



		list.includes(\ratio).if ({

			list = list ++
				[[\signal_amp, \shift_amp]] ++ Pfunc({arg evt;
										var rat;
										rat = evt.at(\ratio);
										//evt.postln;
										[rat, 1 - rat]})
		});

		pairs.notNil.if({ list = list++pairs });

		(locator.notNil &&
			conductor[\xlocation_val].notNil &&
			conductor[\ylocation_val].notNil).if({

			list = list ++
				[[\left_distsance, \left_delay_time, \left_amp,
					\right_distance, \right_delay_time, \right_amp]] ++
									Pfunc({ arg evt;
										locator.get_stereo_information(
												this.xlocation_val, this.ylocation_val,
												evt.at(\amp));
									})

		});

		instrument.notNil.if({
			list = list ++ \instrument ++ instrument.asSymbol;
		});

		list = list ++
			\startFrame ++ 	Pfunc({buf.startFrame}) ++
			\bufnum ++		buf.bufnum ++
			\isPlaying ++		Pfunc({|evt| evt.postln; true}) ++
			\flag ++			Pfunc({ flag })
			;

		postln(*list);

		//list = list++ \evt ++ Pfunc({|event| evt = event});

		list = list++ \evt ++ Pfunc({|event| evt = event; this.pr_addEvent(event)});

		^list;
		*/
	}



	play_pbind_loc { arg instrument, timer;



		pattern = Ptpar([timer.tempo, Pbind (

			\instrument,	instrument,
			\bufnum,		buf.bufnum,
			\dur,		Pfunc({timer.phrase_len * this.dur_val}),
			\amp,		this.amp,
			\startFrame,	0,
			\interval,	this.shift,
			\ratio, 		this.ratio,
			\rate,		this.rate,
			[\signal_amp, \shift_amp], Pfunc({arg evt;
										var rat;
										rat = evt.at(\ratio);
										evt.postln;
										[rat, 1 - rat]}),

			[\left_distsance, \left_delay_time, \left_amp,
				\right_distance, \right_delay_time, \right_amp],
									Pfunc({ arg evt;
										locator.get_stereo_information(
												this.xlocation_val, this.ylocation_val,
												evt.at(\amp));
									})
		)]);
		pattern = pattern.playExt(timer.clock);
		//Routine({pbind = pbind.play}).play(timer.tempoclock);
	}

	play_pbind_pan { arg instrument, timer, multiple = 1, group;
		var nodeID;

		group.isNil.if({ nodeID = 0 }, {nodeID = group.nodeID});

		pattern = Ptpar([/*timer.tempo*/ 0, Pbind (

			\instrument,	instrument,
			\bufnum,		buf.bufnum,
			\dur,		Pfunc({timer.phrase_len * this.dur_val * multiple}),
			\amp,		this.amp,
			\startFrame,	this.buf.startFrame,
			\interval,	this.shift,
			\ratio, 		this.ratio,
			\pan,		this.pan,
			\group,		nodeID,
			[\signal_amp, \shift_amp], Pfunc({arg evt;
										var rat;
										rat = evt.at(\ratio);
										evt.postln;
										[rat, 1 - rat]})


		)]);
		pattern = pattern.playExt(timer.clock);
		//Routine({pbind = pbind.play}).play(timer.tempoclock);
	}

	stop_pbind {

		(pattern.notNil).if ({
			pattern.stop;
			pattern = nil;
			"stop!".postln;
			//evt.set(\amp, 0);
			{
				evt.set(\gate, 0);
			}. try ({ evt = nil});
			events.notNil.if({
				events.do({|e|
					e.notNil.if({
						{
							e.set(\gate, 0);
						}.try({ events.remove(e) });
					}, { events.remove(e)});
				});
			});
		});
		flag = nil;
		this.prune;
	}

	stop {

		this.stop_pbind;
		this.stopBBCut;
		evt.notNil.if({
			{
				evt.set(\gate, 0);
			}.try ({ evt = nil });
		});
		events.notNil.if({
			events.do({|e|
				e.notNil.if({
					{
						e.set(\gate, 0);
					}.try({ events.remove(e);});
				})
			});
		});
	}




	play_once_loc { arg instrument, timer;
		/*
		locator.compute_stereo_information(this.xlocation_val, this.ylocation_val, this.amp_val);

		Routine({
			Synth(instrument, [\bufnum, buf.bufnum, \startFrame, 0,
					\dur, timer.phrase_len * this.dur_val, \left_amp, locator.left_amp,
					\right_amp, locator.right_amp, \left_delay_time,
					locator.left_delay_time]);
		}).play(timer.tempoclock);
		*/
		play_once_grain_loc(instrument, timer, 0);
	}

	play_once_pan { arg instrument, timer;
		/*

		Routine({
			Synth(instrument, [\bufnum, buf.bufnum, \startFrame, 0,
					\dur, timer.phrase_len * this.dur_val, \amp, this.amp_val,
					\pan, this.pan_val]);
		}).play(timer.tempoclock);
		*/
		play_once_grain_pan(instrument, timer, 0);
	}


	play_once_grain_loc { arg instrument, timer, startframe;

		locator.compute_stereo_information(this.xlocation_val, this.ylocation_val, this.amp_val);

		(startframe.isNil).if ({ startframe = buf.startFrame});

		//Routine({
		//	Synth(instrument, [\bufnum, buf.bufnum, \startFrame, 0,
		//			\dur, timer.phrase_len * this.dur_val, \left_amp, locator.left_amp,
		//			\right_amp, locator.right_amp, \left_delay_time,
		//			locator.left_delay_time, \startFrame, startframe]);
		//}).play(timer.tempoclock);
		(instument: instrument, bufnum: buf.bufnum, startFrame: 0,
				dur: timer.phrase_len * this.dur_val, left_amp: locator.left_amp,
				right_amp: locator.right_amp, left_delay_time:
				locator.left_delay_time, startFrame: startframe
		).play(timer.tempoclock);
	}

	play_once_grain_pan { arg instrument, timer, startframe;

		(startframe.isNil).if ({ startframe = buf.startFrame});

		//Routine({
		//	Synth(instrument, [\bufnum, buf.bufnum, \startFrame, 0,
		//			\dur, timer.phrase_len * this.dur_val, \amp, this.amp_val,
		//			\pan, this.pan_val, \startFrame, startframe]);
		//}).play(timer.tempoclock);

		(instrument:instrument, bufnum: buf.bufnum, startFrame: 0,
			dur: timer.phrase_len * this.dur_val, amp: this.amp_val,
			pan: this.pan_val, startFrame: startframe
		).play(timer.tempoclock);
	}

	/*
	play_continuous { arg instrument, timer, startframe, multiple = 1, group ... pairs;

		var list, event, prev, dict;

		(startframe.isNil).if ({ startframe = buf.startFrame});


		list = this.eventPairs(instrument, timer, multiple, group, *pairs);

		dict = IdentityDitcionary.new;

		list.dump;

		list.collect({ |item|
			//var it;
			item.postln;
			item.isKindOf(Pattern).if({
				"pattern".postln;
				item = item.asStream;
			});
			item.isKindOf(Stream).if({
				"stream".postln;
				item = item.next;
			});

			item.isKindOf(SimpleNumber).if({
				item = 0 + item;
			});

			prev.isNil.if({
				prev = item;
			}, {
				dict.put(prev, item);
				prev = nil;
			});

			item;
		});

		//(timer.notNil).if ({

			event = Event(*list).play;
		//});

		this.pr_addEvent(event);
	}
	*/

	//pattern_player { |pclass,


	show { arg argName, x, y, w, h;

		conductor.show();

	}

	//hide {

		//conductor.gui.hide();
	//}

	front {
		conductor.show;
		conductor.gui[\win].front;
	}

	calc_grains_dur_arr { |dur_arr, recurse|

		micro_grains = buf.calc_grains_dur_arr(dur_arr, recurse);
	}

	beatlength_ { |beats|
		buf.buf.isKindOf(BBCutBuffer).if({
			"beats".postln;
			buf.buf.beatlength = beats;
		});
	}

	beatlength{
		^buf.buf.beathlength;
	}

	bps_{ |b|
		buf.buf.isKindOf(BBCutBuffer).if({
			"beats".postln;
			buf.buf.bps = b;
		});
	}


	startBBCut{ |timer, offset, cutproc|

		(cutproc.isNil).if({
			cutproc = BBCutProc11.new;
		});

		this.pr_bbcut = BBCut2(CutBuf2(buf.buf, offset), cutproc).play(timer.clock);
		^bbcut;
	}

	pr_bbcut_{|bb|
		//bbcut.notNil.if({bbcut.stop; bbcut.end;});
		this.stopBBCut;
		bbcut = bb.amp_(conductor.at(\amp));
		^bbcut;
	}

	stopBBCut{
		bbcut.notNil.if({
			bbcut.pause;
			bbcut.stop;
			bbcut.end;
		});
	}

	free {
		//conductor.gui.hide();
		buf.free;
	}

	prune { |num|

		var event;

		num = num ?? events.size;

		num.do({|i|
			event = events.at(i);
			event.isPlaying.not.if({
				events.remove(event);
			})
		})
	}

	pr_addEvent{ |event|

		var dur;

		events.isNil.if({
			events = [];
		});

		dur = event.at(\dur);
		dur.notNil.if({
			(dur > 0.3).if({
				events = events.add(event);
			})
			}, { // unknown dur
				events = events.add(event);
		});

		//(events.size > 30).if({ events.removeAt(0)});
		(events.size > 30).if ({
			this.prune(20);
		});


	}
}
	