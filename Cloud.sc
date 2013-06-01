GrainPicCloud {


	// in this file:

	// change the cursour so it also usies a mapping thing that looks at the current width of the canvas
	// so it also has a .input thing that holds the percent t the end its at




	var <>edgePoints, <boundingRect, <inside, <>settings, <>dur, <>page, pauseRatio, playRatio,
		lowPitchRatio, hiPitchRatio, <>high, <>low, laylaying, <>x, <>colour, <isPlaying;


	*initClass {

		StartUp.add({

			SynthDef( "sineGrains", { arg out = 0, pan = 0, highfreq = 2000, lowfreq = 200,
				grainDur = 0.04, gate = 1, density = 5, db = -20;

				var dust, env, wfreq, freq, mainEnv, sine, panner, amp;

				amp = db.dbamp;
				mainEnv = EnvGen.kr(Env.asr(0.01, 1, 0.01, 1), gate, doneAction:2);
				dust = Dust.kr(density);
				wfreq = Dwhite(lowfreq, highfreq, inf);
				freq = Demand.kr(dust, 0, wfreq);
				env = EnvGen.kr(Env.sine(grainDur, amp), dust > 0, doneAction: 0);

				sine = SinOsc.ar(freq, 0, env * amp);

				panner = Pan2.ar(sine, pan, mainEnv);

				Out.ar(out, panner);

			}).store;
		})
	}



	*new { arg pointArr, pageSize, duration, hi, lo, conductor, colour;
		^super.new.init(pointArr, pageSize, duration, hi, lo, conductor, colour);
	}


	init { arg pointArr, pageSize, duration, hi, lo, conductor, color;

		var minX, minY, maxX, maxY, x, y, count;

		isPlaying = false;
		colour = color;
		colour.isNil.if({colour = Color.rand; colour.alpha = 0.3.rrand(0.8);});

		// Determine the minimums and maximums to find a bounding rectangle

	 	edgePoints = pointArr;

	  	minX = maxX = pointArr.first.x;
	  	minY = maxY = pointArr.first.y;

	  	edgePoints.do ({ arg next;

	  		x = next.x;
	  		y = next.y;

	  		if (x < minX, { minX = x; },

	  			{if ( x > maxX, { maxX = x;  }); });

	  		if (y < minY, { minY = y; },

	  			{if ( y > maxY, { maxY = y;  }); });

	  	});

	  	boundingRect = Rect.fromPoints(Point(minX, minY), Point (maxX, maxY));

		// set some of our objects to external CVs

	  	dur = duration;
	  	page = pageSize;

	  	high = hi;
	  	low = lo;


	  	// get an array of ranges of Y values for every X

	  	inside = [];

	  	count = minX;
	  	{count <= maxX}. while({

	  		inside = inside.add([count, this.discoverYsForX(count)]);
	  		count = count + 1;
	  	});



	  	if (conductor.notNil, {

	  		settings = conductor;

	  	} , {

			// describe a default settings object
		  	settings = Conductor.make({ arg cond, db, pan, density, grainDur, x;

		  		var div, frameDur, lowFreq, highFreq, dur;

			  	db.sp(-20, -100, 20);
			  	pan.spec_(\pan);
			  	density.sp(10, 0.001, 100);
		  		grainDur.sp(0.01509, 0.001, 1);
					x.sp(this.x, boundingRect.left, boundingRect.right, 1, 'linear');

	  			lowFreq = \rest;


	  			cond.pattern_(

	  				Pbind(

	  					\dur, Prout({
	  						inside.size.do({
	  							dur = this.getPixelDur;
	  							dur = dur + dur.sum3rand;
	  							dur.yield;
	  						});
	  					}),


	  					[\highfreq, \lowfreq, \div], Pfunc({

	  						var arr, freqRange, loopArr, popped, left;

							div =1;

							/*
	  						left = boundingRect.left;

	  						inside.do({ arg boundArr, count;
	  							arr = this.getFreqs(left + count);
	  							highFreq = arr.first;
	  							lowFreq = arr.last;
	  							div = lowFreq.size;
	  							[highFreq, lowFreq, div].yield;
	  						});
							*/
								arr = this.getFreqs(x.value);
								[arr.first, arr.last, arr.last.size]
	  					}),


	  					\grainDur, grainDur,
	  					\density, Pfunc({arg evt; density.value / evt[\div]}),
	  					\db,  db,
	  					\pan, pan,
	  					\legato, 2, //1.1,


	  					\instrument, \sineGrains

	  				)
	  			);
	  		});
	  	});

	  }

	getPause {

		// length of puase from starting at the left hand side of the page
		// and this object playing

		^(((boundingRect.left - page.left) / page.right) * dur.value);
	}


	cursor { arg x;


		x.isNil.if({ this.stop }, {
			x = x.value;

			// is this within this cloud?
			if (((x >= boundingRect.left) && (x <= boundingRect.right)), { // yes

				isPlaying.not.if({
						isPlaying = true;
					settings[\x].value = x;
						settings.play;
				//"play".postln;
			}); // start playing

			//x.postln;
			settings[\x].value = x;


			}, {  // we are outside the cloud
				isPlaying.if({isPlaying = false;  settings.stop});
			});
		});
	}

	isInside { arg point;

	  	// is this point within the cloud?

	  	var x, range, y, hi, low, found;

	  	if (boundingRect.containsPoint(point), {

			found = false;

	  		x = point.x;
	  		y = point.y;

	  		range = inside[x - boundingRect.left].last.copy;
	  		if ( range.notNil, {

	  			{range.size > 0}. while({

	  				hi = range.pop;
	  				low = range.pop;
					(hi.notNil && low.notNil).if({
						if ((hi >= y) && (low <= y), {

							found = true;
						});
					} , { "there's a problem here".postln; });
	  			});
	  		});

	  		^found;
	  	});

	  	^false;
	}

	getRanges { arg x;

		// what are the Y values for any given X

		var range;

		if ((x >= boundingRect.left) && (x <= boundingRect.right), {

			range = inside[x - boundingRect.left].last.copy;
		});

		^range;
	}

	getFreqs { arg x;


		// what are the frequency limits for any given X?

		var range, freqRange, his, lows;

		x = x.value;

		range = this.getRanges(x);

		range.notNil.if({
			freqRange = high.value - low.value;
			// Y's are upside down from how you would expect
			range = page.bottom - range;
			range = ((range / page.bottom) * freqRange) + low.value;

			// make sure there's an even number
			if ((range. size % 2) != 0, {
				range = range ++ range.last;
			});

			// return an array of highs and an array of lows
			his = []; lows = [];
			{range.size > 0}.while({
				lows = lows.add(range.pop);
				his = his.add(range.pop);
			});

			^[his, lows];
		}, {
			^[[], []];
		})
		//^range;
	}

	makeHiLowDivArr {

		var x, arr, hilo, div;

		arr = [];
		x = boundingRect.left;

		{x <= boundingRect.right}. while ({

			hilo = this.getFreqs(x);
			div = hilo.last.size;
			arr = arr.add([hilo.first, hilo.last, div]);

			x = x+1;

		});

		^arr;
	}

	getPixelDur {


		// how long does each X last?
		//(dur.value / (page.right - page.left)).postln;
		^(dur.value / (page.right - page.left));

	}


	discoverYsForX { arg x;

		// 'private' method to find Ys for any X

		var returnVal, crossings, y, next, slope, yarr;

		returnVal = [];
		yarr = [];

		if (x < boundingRect.left || x > boundingRect.right, {

			 yarr = nil;
	  	}, {

		  		edgePoints.do({ arg point, index;

		  			// the next point
		  			next = edgePoints[(index + 1) % edgePoints.size];


	  				// is our x included in this line segment?
	  				//["discoverYsForX next.x", next.x, "x", x].postln;

	  				if (((point.x < x) && (next.x >= x)) ||
	  					((point.x > x) && (next.x <= x)), {

	  						// find the equation for the line described by the two edge points

	  						slope = (next.y - point.y) / (next.x - point.x);

	  						// line equation:  y - point.y = slope * (x - point.x)

	  						// if we give our x, find y on the line
	  						// then add it to the array

	  						yarr = yarr.add((slope * (x - point.x)) + point.y);

	  					});
	  				});

	  				// sort the array
	  				yarr = yarr.sort;
	  				// the y cordinates in the array can now be thought of as pairs.
	  				// the first one in the pair is the high part of a range
	  				// and the second one is the low part of a range

	  		});

	  		// if yar is nil, the given x falls outside of our bounding box
	  		// if the array is empty, then there are no points within the shape for the given x
	  		// otherwise, the array is pairs as described above

	  		^yarr;

	  }

	  play {

	  	// Tell our settings object, a Conductor, to play
				isPlaying = true;
	  	^settings.play;
	  }

	  pause {
	  	// Tell our settings object to pause
	  	^settings.pause;
	  }

	  stop {
	  	// Tell our settings object to stop
				isPlaying = false;
	  	^settings.stop;
	  }

}

GrainPicCursor {

	var <startTime, >offset, >duration, >distance, input;

	*new { arg offset = 0, dur = 0, dist = 0;

		^super.new.init(offset, dur, dist);
	}

	init { arg offset = 0, dur = 0, dist = 0;

		startTime = Main.elapsedTime;
		duration = dur;
		distance = dist;
		this.offset = offset;
	}

	reset { arg offset, dur, dist;

		startTime = Main.elapsedTime;

		if (offset.notNil, {
			this.offset = offset;});
		if (dur.notNil, {
			duration = dur; });
		if (dist.notNil, {
			distance = dist;
		});

	}

	timeRunning {

		^ (Main.elapsedTime - startTime);
	}

	x { arg dur, dist;

		var pixelDur, elapsed;

		if (dur.notNil, {
			duration = dur;
		});

		if (dist.notNil, {
			distance = dist;
		});

		if (((duration.isNil) || (distance.isNil)), {
			^0;
		});

		pixelDur = duration / distance;
		elapsed = (Main.elapsedTime - startTime);

		^ ((elapsed / pixelDur) + offset);
	}

	getX { arg dur, dist;
		^this.x(dur, dist);
	}

}

GrainPicController {  //make this a GUI object to handle gui stuff

	var <hiPitch, <lowPitch, <>playAction, <>stopAction, <>dbAction, <duration, editSelect, >callMode, <db,
	controlWindow, <>growAction;

	*new { arg playfunc, stopfunc;
		^super.new.init(playfunc, stopfunc);
	}


	init { arg playfunc, stopfunc;

		var playButton, stopButton, freqRange, vertOffset, horOffset, growButton;

		vertOffset = 20;
		horOffset = 20;

		playAction = playfunc;
		stopAction = stopfunc;


		controlWindow = Window.new("Control Window");
		playButton = Button(controlWindow, Rect(20, vertOffset, 50, 20));
		stopButton = Button(controlWindow, Rect(90, vertOffset, 50, 20));
		growButton = Button(controlWindow, Rect(160, vertOffset, 50, 20));
		editSelect = PopUpMenu(controlWindow, Rect(230, vertOffset, 100, 20));
		vertOffset = vertOffset + 40;
		hiPitch = CV(\widefreq, 2000);
		lowPitch = CV(\widefreq, 300);

		StaticText(controlWindow, Rect(20, vertOffset, 50, 20)).string_("freq");
		//RangeSlider(controlWindow, Rect(70, vertOffset, 150, 20)).connect([lowPitch, hiPitch]);
		[lowPitch, hiPitch].connect(RangeSlider(controlWindow, Rect(70, vertOffset, 150, 20)));
		//SCNumberBox(controlWindow, Rect (225, vertOffset, 50, 20)).connect(lowPitch);
		lowPitch.connect(NumberBox(controlWindow, Rect (225, vertOffset, 50, 20)));
		//SCNumberBox(controlWindow, Rect(280, vertOffset, 50, 20)).connect(hiPitch);
		hiPitch.connect(NumberBox(controlWindow, Rect(280, vertOffset, 50, 20)));
		vertOffset = vertOffset + 40;

		duration = CV.new.sp(60, 1, 1200, 0);
		StaticText(controlWindow, Rect(20, vertOffset, 50, 20)).string_("dur");
		//SCSlider(controlWindow, Rect( 70, vertOffset, 150, 20)).connect(duration);
		duration.connect(Slider(controlWindow, Rect( 70, vertOffset, 150, 20)));
		//SCNumberBox(controlWindow, Rect(225, vertOffset, 50, 20)).connect(duration);
		duration.connect(NumberBox(controlWindow, Rect(225, vertOffset, 50, 20)));
		vertOffset  = vertOffset + 40;

		db = CV(\db); db.value = -6;
		StaticText(controlWindow, Rect(20, vertOffset, 50, 20)).string_("db");
		db.connect(Slider(controlWindow, Rect( 70, vertOffset, 150, 20)));
		db.connect(NumberBox(controlWindow, Rect(225, vertOffset, 50, 20)));
		vertOffset  = vertOffset + 40;
		db.action = {
			dbAction.notNil.if({
				dbAction.value(db.value)
			});
		};

		playButton.states = [["[ > ]"]]; //, ["[ || ]"]];
		stopButton.states = [["[ [] ]"]];
		growButton.states=[["<-->"]];

		playButton.action = { arg state;
			playAction.notNil.if({
				playAction.value(state);
			});
		};

		stopButton.action = { arg state;
			stopAction.notNil.if({
				stopAction.value(state);
			});
		};

		growButton.action = {
			growAction.notNil.if({
				growAction.value();
			});
		};

		editSelect.items = ["Draw", "-", "Edit Single", "Select", "Cut (", "Copy (", "Paste ("];
		editSelect.background_(Color.white);
		editSelect.action = { arg sel;

			//["value", butt.value].postln;

			//stopAction.value;

			if (sel.value == 0, { // draw

				"[Draw]".postln;

				if (callMode.notNil, {
					callMode.value (true, false);
				});
				//drawMode = true;
				//editMode = false;
			//	"true".postln;

			}, {

				if (callMode.notNil, {
					callMode.value (false);
				});
				//drawMode = false;
			//	"false".postln;

				if (sel.value == 2, { // edit
					"[Edit]".postln;
					if (callMode.notNil, {
						callMode.value (false, true);
					});

					//editMode = true;

				});

			});
		};

	controlWindow.view.addAction({ |view, char, modifiers, unicode, keycode, key|

			(key == 0x20).if ({ // space
				playAction.value(true);
			});
		}, \keyDownAction);

		controlWindow.bounds_(Rect(0, 0, 350, vertOffset));

		controlWindow.front;
		controlWindow.alwaysOnTop = true;

	}

	setEditState { arg state;

		editSelect.value = state;
	}

	setEditMode { arg mode;

		// 0 = normal
		// 1 = something selected
		// 2 = something in clipboard

		if (mode == 0, {
			editSelect.items = ["Draw", "-", "Edit Single", "Select", "Cut (", "Copy (",
							"Paste ("];
			editSelect.value = 2;
		}, {
			if (mode == 2, {

				editSelect.items = ["Draw", "-", "Edit Single", "Select", "Cut", "Copy",
								"Paste ("];
				editSelect.value = 3;
			}, {

				editSelect.items = ["Draw", "-", "Edit Single", "Select", "Cut (", "Copy (",
							"Paste"];


			});
		});
	}

	front {

		controlWindow.front;
	}

	dur {

		duration.value;
	}

	dur_ {|dur|

		duration.value = dur;
	}

	alertGrow { |change|

		// change = oldWidth / toAdd;
		// toAdd * change = oldWidth;
		// toAdd = oldWith / change;
		duration.spec.maxval = duration.spec.maxval + (duration.spec.maxval / change);
	}

}







GrainPicScribble {

	 var startPoint, points, <bounds, clouds, window, tablet, drawMode, drawing, bus, <>activeCloud, invert,
	 	>hiPitch, <>lowPitch, >duration, control, editMode, cursor,		selectRect, player,
	<defaultSettings, active_default,
	<playAction, <stopAction, dbAction, semaphore,
	cloudListeners, keyboardListeners,
	<distance;


	*new { arg scribbleSize = /*Rect(40,40,1000,600)*/Rect.newSides(Window.availableBounds.left, Window.availableBounds.top,
		Window.availableBounds.right, Window.availableBounds.bottom -20), defaultCloudSettings, projectorMode = false;

			^super.new.init(scribbleSize, defaultCloudSettings, projectorMode);
	}


	init { arg scribbleSize = /*Rect(40,40,1000,600)*/Window.availableBounds, defaultCloudSettings, projectorMode;

		var scroller;

		bounds = scribbleSize;
		defaultCloudSettings.isKindOf(Function).if({
			defaultSettings = Environment.new;
			defaultSettings.put(\default, defaultCloudSettings);
			active_default = defaultCloudSettings;
		}, {
			defaultSettings = defaultCloudSettings;
		});

		window = Window.new("GrainPic", //Rect.newSides(bounds.left, bounds.top,
					//bounds.right/* + 80*/, bounds.bottom/* + 80*/)
			bounds, scroll:true);

		//tablet = View.new(window, bounds);
		//scroller = ScrollView(window, bounds);
		tablet = UserView(/*scroller*/window, bounds);
		distance = (tablet.bounds.left - tablet.bounds.right).abs;

		invert = projectorMode;

		invert.if({
			window.view.background_(Color.black);
			//scroller.background = Color.black;
		}, {
			window.view.background_(Color.white);
			//scroller.background = Color.white;
		});
		tablet.background = Color.clear;


		points = [];
		window.front;

		projectorMode.if({
			window.fullScreen;
		});

		clouds = Environment.new;
		cloudListeners = [];
		drawMode = true;
		editMode = false;
		drawing = false;


		window.drawFunc = { //was window

			var cx, colour;

			// This is where we tell it how to draw our lines.
			// See the Pen helpfile for more information

			invert.if({
				colour = Color.white;
			}, {
				colour = Color.black;
			});

			colour.set;

			// draw the shape in progress

			this.drawPoints(colour);

			// draw the finished clouds

			this.drawClouds(colour);

			// draw cursor if not nil

			if (cursor.notNil && player.notNil, {

				cx = cursor.getX(/*dist: tablet.bounds.width*/);

				Color.red.set;
				Pen.beginPath;
				Pen.moveTo(Point(cx, bounds.top+20));
				Pen.lineTo(Point(cx - 7, bounds.top /*-10*/));
				Pen.lineTo(Point(cx + 7, bounds.top /*- 10*/));
				Pen.lineTo(Point(cx, bounds.top+20));

				Pen.fill;
				Pen.stroke;
				//"cursor".postln;
				//window.refresh;
			});

			if(selectRect.notNil, {

				this.drawRect.value;
			});


		};


		tablet.mouseUpAction = { arg  view,x,y,//pressure,tiltx,tilty,deviceID, // not supported
							modifiers, buttonNumber,clickCount;

			// called when we lift the mousebutton

			var shape, point, cl;

			if (drawMode && (buttonNumber ==0), {

				drawing = false;
				//["up", x,y,pressure,tiltx,tilty,deviceID, buttonNumber,clickCount].postln;
					tablet.background = Color(x / 300,y / 300,/*tiltx,pressure*/ 0, 0,);
				//t.visible = false;

				// make sure it's a circle
				// or comment these out for lines
				shape = points.pop;
				shape = shape ++ startPoint;
				//points = points.add(shape);

				//cl = GrainPicCloud.new(shape, rect, duration,
					//					hiPitch, lowPitch);

				//if (defaultSettings.notNil, {
				//	cl.settings = defaultSettings.value(cl);
				//});
				//clouds = clouds.add(cl);
			//	this.addCloud(c1);
				this.makeCloud(shape);

			});

			window.refresh;
		};

		tablet.mouseDownAction = { arg  view,x,y,//pressure,tiltx,tilty,deviceID, // not supported
								modifiers, buttonNumber,clickCount;

			// called when we click the mousebutton
			//var activeCloud;

			startPoint = this.makePoint(x, y);

			if (drawMode && (buttonNumber == 0), {
				drawing = true;
				points = points.add( [ startPoint ]) ;

			}, {

				// else, if editMode, select one of the clouds
					if (editMode || (buttonNumber != 0), {
					clouds.values.do({arg shape;

						if( shape.isInside(startPoint), {

							activeCloud = shape;
						});
					});

					if (activeCloud.notNil, {

						activeCloud.settings.show;
					});
				});

			});


			window.refresh;
		};


		tablet.mouseMoveAction = { arg  view,x,y,pressure,//tiltx,tilty,deviceID, // not supported
			modifier, buttonNumber,clickCount;


			// called while moving the mouse aorund with the button depressed.

			var point, shape;

			if (drawing, {

				point = this.makePoint(x, y);
				shape = points.pop;
				shape = shape ++ point;
				points = points.add(shape);

			});

			window.refresh;

		};


		control = GrainPicController.new;
		duration = control.duration;
		hiPitch = control.hiPitch;
		lowPitch = control.lowPitch;
		control.callMode = { arg dr, ed;
			if (dr.notNil, {
				drawMode = dr;});
			if (ed.notNil, {
				editMode = ed;});
		};

		control.growAction = {
			//"grow".postln;
			this.grow();
		};


		keyboardListeners = [
			{|char, modifiers, unicode, keycode, key|

				(key == 0x20).if ({ // space
					control.front;
					//this.play;
				});
			}
		];

		window.view.addAction({ |view, char, modifiers, unicode, keycode, key|

			keyboardListeners.do({ |listener|

				listener.isKindOf(Function).if({
					"function".postln;
					listener.value(char, modifiers, unicode, keycode, key);
				}, /*{
					listener.respondsTo('keyDownAction').if({
						listener.keyDownAction(view, char, modifiers, unicode, keycode, key);
					} , else {
						listener.respondsTo('action').if({
							listener.action(char, modifiers, unicode, keycode, key);
						})
					})
				}*/);
			});
		}, \keyDownAction);



		this.playAction = {};


		this.stopAction = {		};

		//thread safety for adding clouds
		semaphore = Semaphore(1);

	}

	defaultSettings_ { arg settings;

		settings.isKindOf(Function).if({
			defaultSettings = Environment.new;
			defaultSettings.put(\default, settings);
			active_default = settings;
		}, {

			defaultSettings = settings;
		});
	}

	addSetting { arg name, setting;

		defaultSettings.isNil.if({
			defaultSettings = Environment.new;
		});

		defaultSettings.put(name, setting);
	}

	activeSetting_ { arg setting;

		setting.notNil.if({
			setting.isKindOf(Function).not({
				setting = defaultSettings.at(setting.asSymbol);
			});
		});

		setting.notNil.if({
			active_default = setting;
		});
	}

	addKeyboardListener {|listener|

		keyboardListeners.isNil.if({
			keyboardListeners = [listener]
		} , {
			keyboardListeners = keyboardListeners ++ listener;
		});
	}

	removeKeyboardListener { |listener|
		keyboardListeners.notNil.if({
			keyboardListeners.remove(listener);
		})
	}



	playAction_ { arg func;

		playAction = func;
		control.playAction = {this.play; func;};
	}

	stopAction_ { arg func;
		stopAction = func;
		control.stopAction = {this.stop; func;};
	}

	dbAction { ^control.dbAction }
	dbAction_{ arg func;
		control.dbAction = func
	}


	grow { arg percent = 0.25;
		// percent of visible screen, not percent of whole
		var oldWidth, toAdd, newWidth, height, change;

		toAdd = (Window.availableBounds.width /*left - Window.availableBounds.right*/).abs * percent;
		oldWidth = (tablet.bounds.left - tablet.bounds.right).abs;
		newWidth = (oldWidth + toAdd).floor;
		change = oldWidth / toAdd;

		control.alertGrow(change);

		height = bounds.height; //(window.bounds.top - window.bounds.bottom).abs.floor;

		//("new size %\n").postf(newWidth);
		window.setInnerExtent(newWidth, height);
		tablet.resizeTo(newWidth, height);
		distance = newWidth;
		//cursor.distance = newWidth;

		^change;
	}


	drawClouds  { arg color = Color.black;

		var x,y, first, second;
		color.set;

		clouds.values.do ({ arg shape;

			// solid cloud

			Pen.beginPath;

			shape.colour.isNil.if({

				color.set;
			},{
				shape.colour.set;
			});

			Pen.moveTo(shape.edgePoints.first);

			shape.edgePoints.do({ arg next;

					//Pen.lineTo(Point(next.x + 40, next.y + 40));
					Pen.lineTo(next);
			});
			//Pen.lineTo(shape.first);


			shape.colour.isNil.if({
				Pen.stroke;
			}, {
				Pen.fill;
			});
		});

	}

	drawPoints  {arg color = Color.black;

		color.set;
		points.do ({ arg shape;

			Pen.beginPath;
			Pen.moveTo(shape.first);

			shape.do({ arg next;

				//Pen.lineTo(Point(next.x + 40, next.y + 40));
				Pen.lineTo(next);
			});

				//Pen.lineTo(shape.first);

			Pen.stroke;
			Pen.fill;
		});
	}

	drawRect  { arg color = Color.black;

		color.set;
	}

	stop {

			if (player.notNil, { player.stop; "stopped".postln;});
			clouds.values.do({|shape| shape.cursor(nil); });
			cursor = nil;
			window.refresh;
			editMode = true;
			player = nil;
		stopAction.value;
	}

	play {

			var playArr, spause, dist, wait;

			playArr = [];

			//if (state == 1, { // play

			"[Play]".postln;
			//editMode = false;
			//drawMode = false;
			/*
			clouds.do({ arg shape, num; // change for Conductor change
				[num, shape.settings.player.players.first.pattern].postln;
				spause = shape.getPause;
				(spause < 0).if ({ spause = 0; });

				playArr = playArr ++ spause ++ // change for conductor change
					shape.settings.player.players.first.pattern;
				//playArr = playArr ++ 0 ++ shape.settings.players.first.pattern;
			});
			["playArr", playArr].postln;
			player = Ptpar(playArr, 1);
			player = player.play; // change to manage stopping
				//Task.new({
			*/
			//dist = tablet.bounds.right - tablet.bounds.left;
			cursor = GrainPicCursor(bounds.left, duration.value, distance);

			player = Pbind(\quant, 0,
				\freq, Pfunc({
					var x;

					cursor.notNil.if({
					x = cursor.getX(duration.value, distance);
						//x.postln;
						clouds.values.do({arg shape;
							shape.cursor(x);
						});
						\rest
					}, { nil })
				}),
				\dur, Pfunc({(duration.value / distance) / 2}),
				\cursor, Pfunc({cursor}) // stop when the cursor is nil
			);
			player.play;

			{
				//var dist, wait;

				//dist = rect.right - rect.left;


				//cursor = GrainPicCursor(rect.left, duration.value, dist);
				(duration.value / distance).wait;

				//(dist).do({
				{ if(cursor.notNil, {
					cursor.timeRunning <= duration.value;
					}, { false; });
				}. while({
					if (cursor.notNil, {
									//cursor = cursor +1;
						window.refresh;
						(duration.value / distance).wait;
					});

				});
				cursor = nil;
				//editMode = true;
				//control.setEditMode(0);
				window.refresh;
			}.fork(AppClock);
						//}).start;

				//});

		playAction.value;

	}


	makePoint { arg x, y;

		// Offset by 40 because the SCTablet View is offset by 40
	  	// Otherwise, all the drawings will be relative to the edge of the window and not
  		// the edge of the tablet

  		^Point(x + bounds.left, y + bounds.top);
	}

	addCloud { arg cloud, id, settings;


		id.isNil.if({
			id = Date.getDate.stamp;
		});

		id = id.asSymbol;

		// make sure we've got some settings
		settings.notNil.if({
			cloud.settings = settings.value(cloud);
		} , {
			cloud.settings.isNil.if({
				this.pr_active_default;
				active_default.notNil.if({
						cloud.settings = active_default.value(cloud);
					});
			});
		});

		// add the cloud

		fork {
			semaphore.wait;

			{clouds.at(id).notNil}.while ({ // is this ID already used?
				// we will need to throw an error to the network, if we're on one

				// try lengthening the string
				id = id.asString ++ 9.rand;
				id = id.asSymbol;
			});

			clouds = clouds.put(id, cloud);

			semaphore.signal;
		};
	}

	makeCloud { arg pointArr;

		var cl;

		// make sure it's a circle
		(pointArr.first != pointArr.last).if ({

			pointArr = pointArr.add(pointArr.first);
		});

		// make sure not to overwrite last set of points incase we're drawning
		points = points.addFirst(pointArr);

		cl = GrainPicCloud.new(pointArr, bounds, duration,
										hiPitch, lowPitch);

		//if (defaultSettings.notNil, {
		//		cl.settings = defaultSettings.value(cl);
		//});


		this.addCloud(cl, settings:this.pr_active_default);

		window.refresh;

		^cl;
	}

	pr_active_default {

		active_default.notNil.if({
			active_default.isKindOf(Function).not.if({
				defaultSettings.notNil.if({
					defaultSettings.isKindOf(Function).if({
						this.defaultSettings = defaultSettings; // this will do the right thing
						"default settings was a function".warn;
					});
					active_default = defaultSettings.at(active_default.asSymbol);
				});
			});
		} , { //active_default is nil
			defaultSettings.notNil.if({
				defaultSettings.isKindOf(Function).if({
					this.defaultSettings = defaultSettings;
					"defaultSettings was a Fuction".warn;
				});
				active_default = defaultSettings.values.first;
			});
		});

		^active_default;
	}

	pr_alert_cloudListeners { arg cloud, added = true;

		var dead;

		dead = [];

		cloudListeners.do({ arg listener, index;
			listener.notNil.if({
				listener.isKindOfFunction.if({
					listener.value(cloud, 'add', this);
				}, {
					added.if ({
						listener.tryPerform('cloudAdded', [cloud, this]).isNil.if({
							dead = dead.add(index);
						});
					});
				});
			} , {
				dead = dead.add(index);
			});
		});

		// bring out your dead

		dead.sort.reverse.do({ arg corpse;
			cloudListeners.removeAt(corpse);
		});
	}

}