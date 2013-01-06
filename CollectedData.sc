CollectedData {

	var <index, <arr, <minFound, <maxFound, recorded, <>jumpResponder;

  	*new { arg size = 0;
  
  		^super.new.init(size);
  	}
  
  	init { arg size = 0;
  
  		index = 0;
  		arr = Array.newClear(size);
  		//"init".postln;
  		recorded = 0;
  	}
  	
  	
  	record { arg data;
  	
  		//"record".postln;
  		if (( data.isNumber.not) , {
  		
  			if (data.respondsTo('value'), {
  			
  				data = data.value;
  			})
  		});
  		
  		arr.put(index, data);
  		//"put".postln;
  		index = index + 1;
  		index = index % arr.size;
  		//this.post; " ".post; index.postln;
  		(minFound.isNil). if ({
  			minFound = data;
  		} , { // else
  			(data < minFound).if ({
  				minFound = data;
  			})
  		});
  		
  		(maxFound.isNil) . if ({
  			maxFound = data;
  		} , { // else
  			(data > maxFound). if ({
  				maxFound = data;
  			});
  		});
  		
  		recorded = recorded + 1;
  	}
  	
  	latest {
  	
  		^arr.wrapAt(index - 1);
  	}
  	
  	
  	meanSum {
  	
  		var sum, sampSize;
  		
  		sum = 0; sampSize = 0;
  		
  		arr.do({ arg data;
  		
  			data.notNil.if ({
  				sum = sum + data;
  				sampSize = sampSize + 1;
  				//sum.post; " ".post; sampSize.postln;
  			})
  		});
  		
  		^ (sum / sampSize)
  	}
  	
  	min {}
  	
  	max {}
  	
  	normalized_latest {
  	
  		var working_arr, result;
  	
  		(recorded < arr.size) . if ({
  			working_arr = arr.copyRange(0, recorded - 1);
  			result = (working_arr.normalize(0, 1)).wrapAt(index - 1);
  			working_arr = nil;
  		} , {
  			//working_arr = arr;
  			result = (arr.normalize(0, 1)).wrapAt(index - 1);
  		});
  		
  		
  		^result;
  	}
}