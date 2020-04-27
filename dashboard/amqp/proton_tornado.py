import tornado.ioloop
from proton.reactor import Container as BaseContainer
from proton.handlers import IOHandler


class TornadoLoopHandler:

	def __init__(self, loop=None, handler_base=None):
		self.loop = loop or tornado.ioloop.IOLoop.instance()
		self.io = handler_base or IOHandler()
		self.count = 0

	def on_reactor_init(self, event):
		self.reactor = event.reactor

	def on_reactor_quiesced(self, event):
		event.reactor.yield_()

	def on_unhandled(self, name, event):
		event.dispatch(self.io)

	def _events(self, sel):
		events = self.loop.ERROR
		if sel.reading:
			events |= self.loop.READ
		if sel.writing:
			events |= self.loop.WRITE
		return events

	def _schedule(self, sel):
		if sel.deadline:
			self.loop.add_timeout(sel.deadline, lambda: self._expired(sel))

	def _expired(self, sel):
		self.reactor.mark()
		sel.expired()
		self._process()

	def _process(self):
		self.reactor.process()
		if not self.reactor.quiesced:
			self.loop.add_callback(self._process)

	def _callback(self, sel, events):
		if self.loop.READ & events:
			sel.readable()
		if self.loop.WRITE & events:
			sel.writable()
		self._process()

	def on_selectable_init(self, event):
		sel = event.context
		if sel.fileno() >= 0:
			self.loop.add_handler(sel.fileno(), lambda fd, events: self._callback(sel, events), self._events(sel))
		self._schedule(sel)
		self.count += 1

	def on_selectable_updated(self, event):
		sel = event.context
		if sel.fileno() >= 0:
			self.loop.update_handler(sel.fileno(), self._events(sel))
		self._schedule(sel)

	def on_selectable_final(self, event):
		sel = event.context
		if sel.fileno() >= 0:
			self.loop.remove_handler(sel.fileno())
		sel.release()
		self.count -= 1
		if self.count == 0:
			self.loop.add_callback(self._stop)

	def _stop(self):
		self.reactor.stop()
		self.loop.stop()

class Container(object):
	def __init__(self, *handlers, **kwargs):
		self.tornado_loop = kwargs.get('loop', tornado.ioloop.IOLoop.instance())
		kwargs['global_handler'] = TornadoLoopHandler(self.tornado_loop, kwargs.get('handler_base', None))
		self.container = BaseContainer(*handlers, **kwargs)

	def initialise(self):
		self.container.start()
		self.container.process()

	def run(self):
		self.initialise()
		self.tornado_loop.start()

	def stop(self):
		self.container.stop()

	def touch(self):
		self._process()

	def _process(self):
		self.container.process()
		if not self.container.quiesced:
			self.tornado_loop.add_callback(self._process)
