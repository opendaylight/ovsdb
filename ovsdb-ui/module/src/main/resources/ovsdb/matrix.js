define(['app/ovsdb/lib/sylvester'], function() {
  function Transform(a, b, c, d , e, f) {
    if (a)
    this.transform = $M([
      [a, b, c],
      [d, e, f],
      [0, 0, 1]
    ]);
    else
      this.transform = Matrix.I(3);
  }

  Transform.fromString = function(string) {
    if(!string) {
      return new Transform();
    }
    var g = document.createElementNS("http://www.w3.org/2000/svg", 'g');
    g.setAttribute('transform', string);
    var t = g.transform.baseVal.consolidate();
    var m = t.matrix;
    return new Transform(m.a, m.b, m.c, m.d, m.e, m.f);
  }

  Transform.combine = function(ma, mb) {
    var t = new Transform();
    t.transform = ma.transform.x(mb.transform);
    return t;
  }

  Transform.prototype.translate = function(tx, ty) {
    this.transform = $M([[1, 0, tx], [0, 1, ty], [0, 0, 1]]).x(this.transform);
    return this;
  };

  Transform.prototype.rotate = function(deg) {
    var rad = parseFloat(deg) * (Math.PI/180),
    cos = Math.cos(rad),
    sin = Math.sin(rad);

    this.transform = $M([[cos, -sin, 0], [sin, cos, 0], [0, 0, 1]]).x(this.transform);
    return this;
  };

  Transform.prototype.scale = function(x, y) {
    var x = x,
    y = y || x;

    this.transform = $M([[x, 0, 0], [0, y, 0], [0, 0, 1]]).x(this.transform);
    return this;
  };

  Transform.prototype.skew = function(x, y) {
    var alpha = Math.tan(parseFloat(x) * (Math.PI/180)),
     betha = Math.tan(parseFloat(y) * (Math.PI/180));

    this.transform = $M([[1, alpha, 0], [betha, 1, 0], [0, 0, 1]]).x(this.transform);
    return this;
  };

  Transform.prototype.transformPoint = function(x, y) {
    var v = $V([x, y, 1]);
    return this.transform.x(v);
  };

  Transform.prototype.toString = function() {
    return this.transform.inspect();
  }

  return {
    Matrix: Transform // Matrix function name already used by Sylvester
  };
});
