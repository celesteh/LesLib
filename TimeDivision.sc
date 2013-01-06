TimeDivision {

	var <dur, <amp, <subdivisions, <atomic_divisions, <steps;
	
	*new { |dur, amp, steps|
	
		^super.new.init(dur, amp, steps);
	}
	
	init {|duration, amplitude = 0.2, substeps = 1|
	
		dur = duration;
		amp = amplitude;
		steps = substeps;
	}
	
	
	subdivide { | numSections, depth = 1, amp = 0.2|
	
		var base_dur, skip, substeps, strong, weight, first, hash, td;
		
		numSections.notNil.if({	 steps = numSections; });

		(depth > 0 ). if ({
		
			substeps = 0;
			strong = [];

			// measures have to be subdivided in groups of 2, 3, or 4

			{substeps < (steps )}.while({

			
				skip = [2, 3, 4, 5].choose;
			
				((substeps + skip) > steps). if({
					skip = steps - substeps;
				});
			
				strong = strong.add([substeps, skip]);
				substeps = substeps + skip;
			});
		
			first = strong.removeAt(0);
			strong = strong.scramble;
		
			base_dur = dur / (((steps * ((steps + 1) / 2)) * 0.1) + steps);
		
			//subdivisions = Array(numSections);
			hash = Array.newClear(steps + 1);
		
			strong.do({ |item, index|
		
				weight = (1 + (0.1 * index)); 
				td = TimeDivision(weight * base_dur, weight * amp, item.last);
				td.subdivide(item.last, depth - 1);
				hash[item.first] = td;
			}); 						
			
			weight = (1 + (0.1 * (strong.size+ 1)));
			td = TimeDivision(weight * base_dur, weight * amp, first.last);
			td.subdivide(first.last, depth - 1);
			hash[0] = td;
			
			
			// ok now put these into some order
			
			subdivisions = [];
			atomic_divisions = [];
			hash.do ({ |item|
			
				(item.notNil).if ({
					subdivisions = subdivisions.add(item);
					atomic_divisions = atomic_divisions ++ item.chop(substeps, 1, amp);
				});
			});
				
				
		})
	}
	
	
	chop { | numSections, depth|
	
		var base_dur, sections, td, weight;
		
		(depth > 0).if ({
			base_dur = dur / (((numSections * ((numSections + 1) / 2)) * 0.1) + numSections);
		
			sections = [];
		
			(numSections - 1).do ({ |index|
		
				weight = (1 + (0.1 * index));
				td = TimeDivision(weight * base_dur, weight * amp, 1);
				td.chop(numSections, depth - 1);
			
				sections = sections.add(td);
			});
			
			sections = sections.scramble;
			weight = (1 + (0.1 * numSections));
			td = TimeDivision(weight * base_dur, weight * amp, 1);
			td.chop(numSections, depth -1);
			sections = sections.insert(0, td);
		});
		
		^sections;
	}
	
}	
	