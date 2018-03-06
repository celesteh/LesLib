Ploop : ListPattern {
	var <>offset;
	var <>callback;
	var <>everyNloops;
	var <>everyNsteps;
	*new { arg list, repeats=1, offset=0, callback, everyNloops=1, everyNsteps;
		^super.new(list, repeats).offset_(offset).callback_(callback).everyNloops_(everyNloops).everyNsteps_(everyNsteps)
	}
	embedInStream {  arg inval;
		var item, offsetValue;
		offsetValue = offset.value(inval);
		if (inval.eventAt('reverse') == true, {
			block { |break|
			repeats.value(inval).do({ arg j;
				list.size.reverseDo({ arg i;
					item = list.wrapAt(i + offsetValue);
					inval = item.embedInStream(inval);
				});
				(callback.notNil && ( j < (repeats.value(inval) - 1))).if({
					list = callback.value(list);
						list.isNil.if({break.value(nil)})
				});
			});
			}
		},{
				block{|break|
			repeats.value(inval).do({ arg j;
				list.size.do({ arg i;
					item = list.wrapAt(i + offsetValue);
					inval = item.embedInStream(inval);
				});
					(callback.notNil && ( j < (repeats.value(inval) - 1))).if({
						list = callback.value(list);
						list.isNil.if({break.value(nil)})
					});

			});
				}
		});
		^inval;
	}
	storeArgs { ^[ list, repeats, offset, callback ] }
}