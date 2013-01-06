Biquad : UGen {

	*ar { arg in, a0 = 1, a1 = 0, a2 =0, a3 =0, a4 =0, mul = 1.0, add = 0.0;
		^this.multiNew('audio', in, a0, a1, a2, a3, a4).madd(mul, add)
	}
	
	*kr { arg in, a0 = 1, a1 = 0, a2 =0, a3 =0, a4 =0, mul = 1.0, add = 0.0;
		^this.multiNew('control', in, a0, a1, a2, a3, a4).madd(mul, add)
	}
	
}

HarmonicOsc : UGen {

	*ar { arg freq = 440, r = 1, mul = 1.0, add = 0.0;
		^this.multiNew('audio', freq, r).madd(mul, add)
	}
	
	*fr { arg freq = 440, r = 1, mul = 1.0, add = 0.0;
		^this.multiNew('control', freq, r).madd(mul, add)
	}
	
}