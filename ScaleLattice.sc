Ratio

{

	var <numerator, <denominator, <float, <sum, <base, <consonances;
	
	
	*new { arg numerator, denominator, upperbound = 2;
	
		^super.new.init(numerator, denominator, upperbound);
	}
	
	
	
	init { arg num, dem, upper = 2;
	
		numerator = num;
		denominator = dem;
		base = upper;
		
		this.pr_simplify();
	}
	
	
	pr_simplify {
	
		var gcd;
		
		//gcd = numerator.floor.gcd(denominator.floor);
		
		//(gcd.isKindOf(SimpleNumber)).if ({
			
		//	{(gcd > 1)}.while({
		
		//		numerator = numerator / gcd;
		//		denominator = denominator / gcd;	
		//		gcd = numerator.gcd(denominator);
		//	});
		//});

		float = numerator / denominator;
		
		{float < 1}.while({
		
			numerator = numerator * base;
			float = numerator / denominator;
		});
		
		
		{float > base}.while ({
		
			denominator = denominator * base;
			float = numerator / denominator;
		});
		
		sum = numerator + denominator;
		

	}	
	
	tranpose { |rat|
	
		var num, dem;


		num = numerator * rat.numerator;
		dem = denominator * rat.denominator;
		
		// take the base from this
		
		^Ratio(num, dem, base)
		
	}
	
	
	digestibility { |rat|
		
		var result;
		
		// ideally all ratios involved would have the same base, 
		// but we'll use this one and not check
		
		result = Ratio(numerator * rat.denominator, denominator * rat.numerator, base);
		^result.sum;
	
	}
	
	
	addConsonances { | scale_arr|
	
		var digest_arr;
		
		digest_arr = [];
		consonances = [];
		
		scale_arr.do({ arg item;
		
			(item.float != float).if ({
		
				//digest_arr = digest_arr.add([this.digestibility(item), item]);
				consonances = consonances.add(ScaleInterval(this, item, scale_arr));
			});
		});
		
		//digest_arr = digest_arr.sort({arg a, b; a.at(0) < b.at(0) });
		consonances = consonances.sort({arg a, b; a.digestibility < b.digestibility});
		
		//consonances = digest_arr;
		
		
		//digest_arr.do({ | item|
		
		//	consonances = consonances.add(item.last);
		//});
	
	}		

	getNconsonances { |num|
	
		var arr;
		
		arr = [];
		(num > consonances.size).if ({ num = consonances.size});
		
		num.do({ |index|
		
			//arr = arr.add((consonances[index]).float);
			arr = arr.add((consonances[index].degree2).float);
		});
		
		^arr;
	}
	
	
	consonanceAt { |num|
	
		^ ((consonances[num]).degree2.float);
	}
	
	
	consonantRatioAt { |num|
	
		^(consonances[num].degree2);
	}
	
	consonantIntervalAt { |num|
	
		^(consonances[num]	);
	}
	
	consonsantTriadAt { | num|
	
		var deg2, deg3;
		
		deg2 = consonances[num].degree2.asFloat;
		deg3 = consonances[num].triadAsFloat;
		^[float, deg2, deg3]
	}
	
	asFloat {
	
		^float;
	}

	
}

ScaleInterval {

	var <degree1, <degree2, <digestibility, consonances;
	
	
	*new { |rat1, rat2, scale|
	
		^super.new.init(rat1, rat2, scale);
	}
	
	
	init { |rat1, rat2, scale|
		
		degree1 = rat1;
		degree2 = rat2;
		
		digestibility = rat1.digestibility(rat2);
		
		//scale.notNil.if ({
		
		//	(scale.size > 0).if ({
				this.addScale(scale);
		//}) })
	
	}
	
	addScale { |scale|
	
		var lower, higher, num, dem, arr, scale_arr, itnum, itdem, digest;
		
		(degree1.float < degree2.float).if ({
		
			lower = degree1;
			higher = degree2;
		} , {
		
			lower = degree2;
			higher = degree1;
		});
		
		num = lower.numerator * higher.denominator;
		dem = lower.denominator * higher.numerator;
		
		arr = this.pr_simplify(num, dem);
		num = arr.first;
		dem = arr.last;

		digestibility = num + dem;

		scale.notNil.if ({ (scale.size > 0).if ({
			
			scale_arr = [];
				
			scale.do({ |item|
			
			
				((item.float != lower.float) && (item.float != higher.float)).if ({
				
					("Rats" + lower.numerator ++ "/" ++ lower.denominator + "/" +
						higher.numerator ++ "/" ++  higher.denominator + "*" +
						item.numerator ++ "/" ++ item.denominator).postln;
						
					itnum = num * item.numerator;
					itdem = dem * item.denominator;
					arr = this.pr_simplify(itnum, itdem);
					itnum = arr.first;
					itdem = arr.last;
					digest = itnum + itdem;
					scale_arr = scale_arr.add([digest, item]);
				});
			});
			scale_arr = scale_arr.sort({arg a, b; a.first < b.first});
			
			consonances = [];
			
			scale_arr.do({ |item|
				
				consonances = consonances.add(item.last);
			});
				
		}) })
		
		
		
	}
	
	
	triad {
	
		^(consonances[0]);
	}
	
	triadAsFloat {
	
		^(consonances[0].asFloat);
	}
	
		
	
	pr_simplify { | num, dem|	

		var gcd, base, flag;

		gcd = num.floor.gcd(dem.floor);
		
		//gcd.postln;
		
		("before gcd loop num is" + num + "dem is" + dem + "gcd is" + gcd).postln;
		//("gcd" + gcd).postln;
		
		flag = gcd.notNil;
		flag.if({ flag = gcd.isKindOf(SimpleNumber)});
		flag.if({ flag = (gcd > 1)});
		
		{flag}.while({
		
			num = num / gcd;
			dem = dem / gcd;	
			gcd = num.floor.gcd(dem.floor);
			("num is" + num + "dem is"+ dem + "gcd is" + gcd).postln;
			flag = gcd.notNil;
			flag.if({ flag = gcd.isKindOf(SimpleNumber)});
			flag.if({ flag = (gcd > 1)});
		
		});
		
		"after gcd loop".postln;
		
		base = degree1.base;
		
		{(num/dem) > base}.while ({
		
			dem = dem * base;
			
		});
	
		^[num, dem]
	}
}

ScaleLattice {

	var <steps, consonances;

	*new{ arg pairs, base = 2;
	
		^super.new.init(pairs, base);
	}
	
	*newFromRatios { arg ... ratioArr;
	
		^super.new.fromRatios( ratioArr);
	}
	
	
	init { |pairs, base = 2|
	
		var rat;
		
		steps = [];
		
		pairs.do { | tuple|
		
			rat = Ratio(tuple.first, tuple.last, base);
			
			steps = steps.add(rat);
		};
		
		this.sort;
		
	}
	
	
	fromRatios { |ratioArr|
	
		steps = [];
		
		ratioArr.do({ |rat|
		
			steps = steps.add(rat);
		});
		
		this.sort;
	}
	
	
	sort {
	
		/*
		var sort_steps, last, placed, /*tuned,*/ step;
		
		(steps.notNil && steps.size > 0).if ({
		
			step = steps.pop;
			sort_steps = [step];
			consonances = [step];
		
			steps.do({ |degree|
			
				last = 0;
				placed = false;
				//tuned = false;
				
				sort_steps.do({ |sort_degree, index|
				
					((degree.float > sort_degree.float) && placed.not).if ({
					
						sort_steps = sort_steps.insert(index, degree);
						placed = true;
					})
				});
				
				(placed.not).if ({
				
					sort_steps = sort_steps.add(degree);
				});
				
				placed = false;
				
				consonances.do({ |con_degree, index|
				
					((con_degree.sum > degree.sum) && placed.not). if ({
					
						consonances = consonances.insert(index, degree);
						placed = true;
					})
				});
				
				(placed.not).if({
				
					consonances = consonances.add(degree);
				});
			});
		});
		*/
		steps = steps.sort({arg a, b; a.float < b.float});
		
		// make sure we've got 1/1
		(steps[0].float != 1).if ({
		
			steps = steps.insert(0, Ratio(1, 1, steps[0].base));
		});
		
		steps.do({ |degree|
		
			degree.addConsonances(steps);
			"loop".postln;
		});
		
	}
	
	
	getNsteps { |num|
	
		var arr;
		
		arr = [];
		
		(num > steps.size).if ({ num = steps.size});
		
		num.do({ |index|
		
			arr = arr.add((steps[index]).float);
		});
		
		^arr;
	}
	
	
	at { |num|
	
		^ ((steps[num]).float);
	}
	
	getNconsonances { |num|
	
		/*
		var arr;
		
		arr = [];
		(num > steps.size).if ({ num = steps.size});
		
		num.do({ |index|
		
			arr = arr.add((consonances[index]).float);
		});
		
		^arr;
		*/
		
		^(steps[0].getNconsonances(num));
	}
	
	
	consonanceAt { |num|
	
		//^ ((consonances[num]).float);
		^(steps[0].consonanceAt(num));
	}

	getIstepsFromJconsonance{ | i, j|
	
		var degree, index, arr, note, freq;
		
		//degree = consonances[j];
		degree = steps[0].consonantRatioAt(j);
		
		index = steps.indexOf(degree);
		arr = [];
		
		i.do({
		
			note = (steps.wrapAt(index));
			freq = note.float;
			//freq = freq + (((index % steps.size) -1) * (note.base - 1) * freq);
			arr = arr.add(freq);
			index = index+ 1
		});
		
		^arr;
	}
	
	getIstepsBelowJconsonance{ | i, j|
	
		var degree, index, arr, note, freq;
		
		//degree = consonances[j];
		degree = steps[0].consonantRatioAt(j);
		
		index = steps.indexOf(degree);
		arr = [];
		
		i.do({
		
			note = (steps.wrapAt(index));
			freq = note.float;
			//freq = freq + (((index % steps.size) -1) * (note.base - 1) * freq);
			arr = arr.add(freq);
			index = index- 1
		});
		
		^arr;
	}

	getNstepsAboveRatio { |num, rat|
	
		var degree, index, arr, note, freq;
		
		//degree = consonances[j];
		//degree = steps[0].consonantRatioAt(j);
		
		index = steps.indexOf(rat);
		arr = [];
		
		num.do({
		
			note = (steps.wrapAt(index));
			freq = note.float;
			//freq = freq + (((index % steps.size) -1) * (note.base - 1) * freq);
			arr = arr.add(freq);
			index = index+ 1
		});
		
		^arr;
	
	}
	
	getNStepsBelowRatio { |num, rat|
		var degree, index, arr, note, freq;
		
		//degree = consonances[j];
		//degree = steps[0].consonantRatioAt(j);
		
		index = steps.indexOf(rat);
		arr = [];
		
		num.do({
		
			note = (steps.wrapAt(index));
			freq = note.float;
			//freq = freq + (((index % steps.size) -1) * (note.base - 1) * freq);
			arr = arr.add(freq);
			index = index- 1
		});
		
		^arr;
	
	}
	
	pr_index_of { |float|
	
		var index, found, result;
		
		found = false;
		index = 0;
		result = -1;
		
		{found.not && (index < steps.size)}.while ({
		
			//("looking" + steps[index].float + "and" + float).postln;
		
			(steps[index].float == float). if({
			
				found = true;
				result = index;
			} , {
			
				(float < steps[index].float). if ({
				
					//it's sorted, so skip the rest;
					//index = steps.size;
					found = true;
				});
			});
			
			index = index + 1;
		}); 
		
		^result;
	}
	
	getNStepsAboveFloat { |num, float|
	
		var index, arr, note, freq;
		
		index = pr_index_of(float);
		
		arr = [];
		
		num.do({
		
			note = (steps.wrapAt(index));
			freq = note.float;
			//freq = freq + (((index % steps.size) -1) * (note.base - 1) * freq);
			arr = arr.add(freq);
			index = index+ 1
		});
		
		^arr;

	
	}
	
	getNStepsBelowFloat { |num, float|
	
		var index, arr, note, freq;
		
		index = pr_index_of(float);
		arr = [];
		
		num.do({
		
			note = (steps.wrapAt(index));
			freq = note.float;
			//freq = freq + (((index % steps.size) -1) * (note.base - 1) * freq);
			arr = arr.add(freq);
			index = index- 1
		});
		
		^arr;
	

	}

	getNconsonancesFromFloat { |num, float|
	
		var index, arr;
		
		//^ ((consonances[num]).float);
		//^(steps[0].consonanceAt(num));
		
		index = this.pr_index_of(float);
		arr = [];

		^(steps[index].getNconsonances(num));

	}

	consonanceAtFloat { |index, float|
	
		var con_index, arr;
		
		//^ ((consonances[num]).float);
		//^(steps[0].consonanceAt(num));
		
		con_index = this.pr_index_of(float);
		arr = [];

		//^(steps[index].getNconsonances(num));

		//^ ((consonances[num]).float);
		^(steps[con_index].consonanceAt(index));
	}
	
	triadAtFloat { |float|
		var index, arr;
		
		//^ ((consonances[num]).float);
		//^(steps[0].consonanceAt(num));
		
		index = this.pr_index_of(float);
		//arr = [];

		//^(steps[index].getNconsonances(num));

		//^ ((consonances[num]).float);
		//^(steps[con_index].consonanceAt(index));
		^(steps.wrapAt(index).consonsantTriadAt(0));
	}
	
	
}
					