~movieView = BosqueMovieView.new;
~movieView.makeGUI;
~movieView.path = "/Users/rutz/Desktop/Interface3/Calig/1c.mov";
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
