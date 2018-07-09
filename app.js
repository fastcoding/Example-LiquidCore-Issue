const createError = require('http-errors');
const os=require('os')
const express = require('express');
var cookieParser = require('cookie-parser');
const logger = require('morgan');
const path = require('path');
var app = express();
require('express-ws')(app)
const workRouter = require('./routes/work');
const indexRouter = require('./routes/index');
const usersRouter = require('./routes/users');
// view engine setup
if (os.platform() === 'android') {
	app.set('views', path.join('/home/local/web', 'views'));
}else{
  app.set('views', path.join(__dirname, 'views'));
}
app.engine('pug', require('pug').__express)
app.set('view engine', 'pug');

app.use(logger('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
app.use(cookieParser());

if (os.platform() === 'android') {
    app.use(express.static('/home/local/web/public'))
}else{
	  app.use(express.static(path.join(__dirname, 'public')));
}

app.use('/', indexRouter);
app.use('/ws',workRouter);
app.use('/users', usersRouter);

// catch 404 and forward to error handler
app.use(function(req, res, next) {
  next(createError(404));
});

// error handler
app.use(function(err, req, res, next) {
  // set locals, only providing error in development
  res.locals.message = err.message;
  res.locals.error = req.app.get('env') === 'development' ? err : {};

  // render the error page
  res.status(err.status || 500);
  res.render('error');
});

module.exports = app;
