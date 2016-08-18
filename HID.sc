// By Celeste Hutchins 2006
// Released under the GPL
/*
HIDDeviceElementExt : HIDDeviceElement {

	var <value, last, <>threshold, <>action, <>name, <minFound, <maxFound;

	*new { arg type, usage, cookie, min, max, name, threshold = 0.05;

		^super.newCopyArgs(type, usage, cookie, min, max, name, threshold).init;
	}

	*copy { arg element;

		^super.newCopyArgs(element.type, element.usage, element.cookie,
			element.min, element.max).init;
	}


	init { arg type, usage, cookie, min, max, name, threshold;

		//super.init(type, usage, cookie, min, max);
		//this.name = name;
		//this.threshold = threshold;
		//spec = ControlSpec(-1, 1, default: 0);
		// last is impossible number, so changed is true the first time
		last = this.min - 100;
		// if threshold doesn't initialize for some reason

		if (this.threshold.isNil, {
			this.threshold = 0.05;
		});
	}


	hidAction { arg vendorID, productID, locID, cookie, value;

		var scale, changed;

		//[last, minFound, maxFound].postln;

		changed = nil;

		(cookie == this.cookie).if({

			// first do some statistics
			if (minFound.isNil, { minFound = value },
				{ if (value < minFound, { minFound = value })});

			if (maxFound.isNil, { maxFound = value },
				{ if ( value > maxFound, { maxFound = value })});

			//[((value - last)/(max-min).abs), this.cookie, cookie].postln;

			(((value - last)/(max-min)).abs > threshold).if ({

				last = value;
				(action.notNil).if ({
					this.action.value(this, vendorID, productID, locID, cookie, value);
				});
				// something has changed, so return true
				changed = this;
				//spec.dump;
				//[name, val, min, max,"scale is", scale, value].postln;
			}, {
				// no change
				changed = nil;
			});
		});

		^changed;
	}

	// the reported mins and maxes from the device are not always true.  let users override
	min_ { arg newmin;

		min = newmin;
	}

	max_ { arg newmax;

		max = newmax;
	}

	scale { arg value;

		value = value - min;
		value = value / (max - min);
		^value;
	}

}

HIDElementGroup {

	var items, <>action, <value, <>name;

	// value is the scaled result of the last element to get input

	*new {

		^super.new.init;
	}


	init {

		items = [];
	}


	add { arg elem;

		items = items.add(elem);
	}


	hidAction { arg vendorID, productID, locID, cookie, value;

		var item, changed;

		changed = false;

			block { | break|
				items.do ({ arg element;

					element.notNil.if ({
						//element.name.postln;
						changed = element.hidAction (vendorID, productID, locID, cookie, value);
						// call action if changed
						(changed.notNil).if ({

							value = element.value;
							(action.notNil).if ({
								this.action.value(changed, vendorID, productID, locID,
											cookie, value);
							});
							// and break rather than go through everything

							break.value(changed); });
					});
				}, { "nil".postln; });
			}


		//item = items.at(cookie.asSymbol);

		//(item.notNil).if ({

		//	changed = item.hidAction (vendorID, productID, locID, cookie, val);

		//	(changed && (action.notNil)). if ({

		//		value = item.value;
		//		this.action.value(item.value, cookie, vendorID, productID, locID, val);
		//	});
		//});

		^changed;
	}

	min_ { arg min;

		// set minimum for every member element

		items.do ({ arg elem;

			elem.min = min;
		});

	}

	max_ { arg max;

		// set maximum for every member element

		items.do ({ arg elem;

			elem.max = max;
		});

	}

	threshold_ { arg thresh;

		// set threshold for every member element

		items.do ({ arg elem;

			elem.threshold = thresh;
		});

	}



}


HIDDeviceExt : HIDDevice {

	var <>action, <deviceSpec, <dict, <>nodes;

	*new{arg manufacturer, product, usage, vendorID, productID, locID;
		^super.newCopyArgs(manufacturer, product, usage, vendorID, productID, locID).init;
	}

	init {

		this.prBuildDict;
		nodes = elements;

	}

	/*
	config { arg func;

		var obj, args, names;
		#args, names = this.configArgs(func);
		func.valueArray(this, args);
	}


	configArgs { arg func;
		var argList, size, names, argNames;
		var theClassName, name, obj;

		size = func.def.argNames.size;
		argList = Array(size);
		argNames = Array(size);
		names = func.def.argNames;
		// first arg is the HID to config, subsequent are HiDElementGroups
		if (size > 1, {
			1.forBy(size - 1, 1, { arg i;
				name = names.at(i).asSymbol;
				argNames = argNames.add(name);
				//theClassName = func.def.prototypeFrame.at(i) ? \CV;
				//obj = theClassName.asClass.new;
				//this.put(name,obj);
				obj = HIDElementGroup.new;
				argList = argList.add(obj);
			});
		});
		^[argList, argNames];

	}
	*/





	hidAction { arg vendorID, productID, locID, cookie, value;

		var changed;

		((vendorID == this.vendorID) && (productID == this. productID)
			&& (locID == this. locID)) . if ({

			//(nodes.isNil). if ({

			//	this.createNodeList;
			//});

			block { | break|
				nodes.do ({ arg element;

					changed = element.hidAction (vendorID, productID, locID, cookie, value);
					// call action if changed
					(changed.notNil).if ({

						(action.notNil).if ({
							this.action.value(changed, vendorID, productID,
											locID, cookie, value);
						});
						// and break rather than go through everything

						break.value(changed); });
				});
			}


		});
	}



	deviceSpec_ { arg spec;

		var symbol;

		deviceSpec = spec;

		symbol = product.asSymbol;

		(HIDDeviceServiceExt.deviceSpecs.at(symbol).isNil). if ({
			// it doesn't know about this spec, so add it
			HIDDeviceServiceExt.deviceSpecs.put(symbol, spec);
		});
		//(HIDDeviceServiceExt.deviceDict.at(symbol).isNil). if ({
			// list us in the dictionary
		//	HIDDeviceServiceExt.deviceDict.put(symbol, this);
		//});

		this.prBuildDict;
	}





	setActionByCookie { arg cookie, action;

		elements.do ({ arg item;

			(item.cookie == cookie). if ({

				item.action = action;
			});
		});
	}


	setActionBySymbol { arg symbol, action;

		var element;

		element = dict[symbol];
		if (element.notNil, {
			element.action = action;
		})
	}

	/*
	setSpecByCookie { arg cookie, controlSpec;

		elements.do ({ arg item;

			(item.cookie == cookie). if ({

				item.spec = controlSpec;
			});
		});
	}

	setSpecBySymbol  { arg symbol, controlSpec;

		var element;

		element = dict[symbol];

		if (element.notNil, {
			element.spec = controlSpec;
		})
	}
	*/

	get { arg symbol;

		^dict[symbol];
	}

	postln {

		("" ++ manufacturer + product + usage + vendorID + productID + locID + version
			+ serial + elements + super.post).postln;
	}

	//private:
	prAddElement{arg type, usage, cookie, min, max;
		elements = elements.add(HIDDeviceElementExt(type, usage, cookie, min, max));

	}

	prBuildDict {

		var key, cookie;

		//"building dict".postln;

		(deviceSpec.notNil).if ({

			dict = IdentityDictionary(elements.size);
			elements.do ({ arg item;

				//item.cookie.postln;

				key = deviceSpec.findKeyForValue(item.cookie);

				if (key.notNil, {
					item.name = key;
					dict.put(key, item);
					//[key, item].postln;
				});
			});
		});
	}




}


HIDDeviceServiceExt : HIDDeviceService {

	classvar <deviceDict, <>action;

	* initClass {

		super.initClass;
		deviceDict = IdentityDictionary.new;
		super.action_({arg vendorID, productID, locID, cookie, val;

			this.hidAction(vendorID, productID, locID, cookie, val);
		});
	}


	// overridden to assign a deviceSpec and to use my Ext classes
	*buildDeviceList{arg usagePage=1, usage=4;
		var devlist, elelist, spec;
		devices = Array.new;
		devlist = this.prbuildDeviceList(usagePage, usage);
		devlist ?? {"HIDDeviceService: no devices found".warn; ^nil};
		devlist.do({arg dev;
			var newdev;
			newdev = HIDDeviceExt(dev.at(0), dev.at(1), dev.at(2), dev.at(3), dev.at(4), dev.at(5));
			elelist = this.prbuildElementList(newdev.locID,
				Array.newClear(HIDDeviceServiceExt.prGetElementListSize(newdev.locID)));
			elelist.do({arg ele;
				if(ele.notNil){
					newdev.prAddElement(ele.at(0), ele.at(1), ele.at(2), ele.at(3), ele.at(4));
				};
			});

			spec = deviceSpecs.at(newdev.product.asSymbol);
			if(spec.notNil, {
				newdev.deviceSpec_(spec)
			});
			devices = devices.add(newdev);
			deviceDict.put(newdev.product.asSymbol, newdev);
		});
		initialized = true;
	}


	// overridden to allow more nuanced action handling
	*hidAction{arg vendorID, productID, locID, cookie, val;

		//"hidAction".postln;
		if (this.action.notNil, {
			this.action.value(vendorID, productID, locID, cookie, val);
		});

		devices.do({ arg dev;

			dev.hidAction(vendorID, productID, locID, cookie, val);
		});
	}

	*setAction { arg device_symbol, element_symbol, action;

		var device, element;

		device = deviceDict[device_symbol];

		if ( device.notNil, {

			if ( element_symbol.notNil, {

				device.setActionBySymbol(element_symbol, action);
			} , {

				device.action = action;
			})
		})
	}

	*queueDeviceByName { arg symbol;

		var dev;

		dev = deviceDict[symbol];
		if (dev.notNil, {
			dev.queueDevice;
		});
	}

}
	*/