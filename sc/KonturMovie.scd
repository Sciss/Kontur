~movieView = BosqueMovieView.new;
~movieView.makeGUI;
//~movieView.path = "/Users/rutz/Desktop/Interface3/Calig/1c.mov";
//~movieView.synced = true;
// (
~moviePos = 0;
~movieUpd = OSCpathResponder( nil, [ '/kontur', \transport ], { arg time, resp, msg;
	switch( msg[Ê3 ],
	\pos, { ~moviePos = msg[ 4 ] + ~movieView.offset; { ~movieView.position = ~moviePos }.defer },
	\play, { { ~movieView.play( ~moviePos )}.defer },
	\stop, { { ~moviePos.stop }.defer }
	);
}).add;
// )

// for section 3
~movieView.offset = -13.0;
~movieView.path = "/Users/rutz/Desktop/Interface3/Calig/3c.mov";

// for section 4
~movieView.offset = -28.0;
~movieView.path = "/Users/rutz/Desktop/Interface3/Calig/4c.mov";

// for section 5
~movieView.offset = -1.0;
~movieView.path = "/Users/rutz/Desktop/Interface3/Calig/5c.mov";

// for section 6
~movieView.offset = 0.0;
~movieView.path = "/Users/rutz/Desktop/Interface3/Calig/6c.mov";
