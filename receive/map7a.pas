unit map7a;

interface

uses
  Classes, Graphics, Types, ExtCtrls,
  city10;

type
  TMap = class
  private
    cx,cy, nview, nmap: Float;
    fCity: TCity;
    fPaint: TPaintBox;
    property paint: TPaintBox read fPaint;
    procedure SetCity(value: TCity);
    function GetView: Float;
    function isChanged: Boolean;
    procedure Resize;
  public
    x,y,z: Float;
    draw: TBitmap;
    AfterPaint: TNotifyEvent;
    property city: TCity read fCity write SetCity;
    property view: Float read GetView;
    procedure ShiftTo(dx,dy, dz: Float);
    procedure Shift;
    function Coord(ax,ay: Float): TPoint;
    function ACoord(vx,vy: Integer): TPoint;
    procedure DrawClear;
    procedure DrawGrid;
    procedure DrawVertex(p: TPoint; s: Integer; circle: Boolean = false);  overload;
    procedure DrawVertex(v: TVertex; s: Integer = 1; c: TColor = -1);  overload;
    procedure DrawVertexes(l: TLine; dots: Boolean = false; c: TColor = -1);
    procedure DrawLines;
    procedure Redraw;
    constructor Create(PaintBox: TPaintBox);
    destructor Destroy;  override;
    procedure Repaint(Sender: TObject);
  end;

implementation

uses
  Math, Dialogs, SysUtils, Controls;

function g: string;
var
  a: string;
  i: Integer;
begin
  a:='';
  for i:=1 to Random(6)+2 do
    a:=a+Chr(Random(26)+65);
  g:=a;
end;

constructor TMap.Create(PaintBox: TPaintBox);
begin
  inherited Create;
  draw:=TBitmap.Create;
  draw.PixelFormat:=pf24bit;
  fPaint:=PaintBox;
  paint.OnPaint:=Repaint;
  z:=1;  nmap:=1;
end;

destructor TMap.Destroy;
begin
  draw.Free;
  inherited Destroy;
end;

procedure TMap.Resize;
begin
  draw.Width:=paint.Width;
  draw.Height:=paint.Height;
  cx:=draw.Width/2;
  cy:=draw.Height/2;
  nview:=(cx+cy)/2;
  Redraw;
end;

function TMap.isChanged: Boolean;
begin
  Result:=false;
  if (paint.Width<>draw.Width) or (paint.Height<>draw.Height) then begin
    Resize;
    Result:=true;
  end;
end;

procedure TMap.SetCity(value: TCity);

  procedure Check(x: Float);
  begin
    if Abs(x)>nmap then
      nmap:=Abs(x);
  end;

var
  i,j: Integer;
  l: TLine;
begin
  fCity:=value;
  nmap:=1;
  for i:=0 to City.List.Count-1 do begin
    l:=City.List[i] as TLine;
    for j:=0 to l.Vertexes.Count-1 do begin
      Check(l.Vertexes[j].x);
      Check(l.Vertexes[j].y);
    end;
  end;
  x:=0;  y:=0;
end;

function TMap.GetView: Float;
begin
  Result:=z*nview/nmap;
end;

procedure TMap.Repaint;
var
  r: TRect;
begin
  if not isChanged() then begin
    r:=draw.Canvas.ClipRect;
    paint.Canvas.CopyRect(r, draw.Canvas, r);
  end;
end;

procedure TMap.Shift;
begin
  Redraw;
end;

procedure TMap.ShiftTo(dx,dy, dz: Float);
begin
  x:=x+dx;
  y:=y+dy;
  z:=z*dz;
  Shift;
end;

function TMap.Coord(ax,ay: Float): TPoint;
var
  f: TPoint;
begin
  f.x:=Round( (ax-x)*view+cx );
  f.y:=Round( (ay-y)*view+cy );
  Result:=f;
end;

function TMap.ACoord(vx,vy: Integer): TPoint;
begin
  Result.x:=Round( (vx-cx)/view+x );
  Result.y:=Round( (vy-cy)/view+y );
end;

procedure TMap.DrawClear;
begin
  with draw.Canvas do begin
    Brush.Color:=clGray;
    Brush.Style:=bsSolid;
    FillRect(ClipRect);
  end;
end;

procedure TMap.DrawGrid;
var
  v1, v2: TPoint;
  v: Integer;
  q: TPoint;
begin
  v1:=ACoord(0, 0);
  v2:=ACoord(draw.Width, draw.Height);
  if Abs(v1.x-v2.x)*6<(draw.Width) then
    with draw do begin
      Canvas.Pen.Color:=clGray+$090909;
      for v:=v1.x to v2.x do begin
        q:=Coord(v, 0);
        Canvas.MoveTo(q.x, draw.Height);
        Canvas.LineTo(q.x, 0);
      end;
      for v:=v1.y to v2.y do begin
        q:=Coord(0, v);
        Canvas.MoveTo(draw.Width, q.y);
        Canvas.LineTo(0, q.y);
      end;
    end;
end;

procedure TMap.DrawVertex(p: TPoint; s: Integer; circle: Boolean = false);
begin
  if circle then
    draw.Canvas.Ellipse(p.X-s, p.Y-s, p.X+s, p.Y+s)
  else
    draw.Canvas.Rectangle(p.X-s, p.Y-s, p.X+s, p.Y+s);
end;

procedure TMap.DrawVertex(v: TVertex; s: Integer = 1; c: TColor = -1);
var
  p: TPoint;
begin
  if c<>-1 then
    draw.Canvas.Pen.Color:=c;
  p:=Coord(v.x, v.y);
  DrawVertex(p, s);
end;

procedure TMap.DrawVertexes(l: TLine; dots: Boolean = false; c: TColor = -1);
var
  i: Integer;
  p: TPoint;
begin
  if c<>-1 then
    draw.Canvas.Pen.Color:=c;
  for i:=0 to l.Vertexes.Count-1 do begin
    p:=Coord(l.Vertexes[i].x, l.Vertexes[i].y);
    if i=0 then
      draw.Canvas.MoveTo(p.X, p.Y)
    else
      draw.Canvas.LineTo(p.X, p.Y);
    if dots then
      DrawVertex(p, 1);
  end;
end;

procedure TMap.DrawLines;
var
  i: Integer;
begin
  draw.Canvas.Pen.Color:=clSilver;
  for i:=0 to city.List.Count-1 do
    DrawVertexes(city.List[i] as TLine, z>15);
end;

procedure TMap.Redraw;
begin
  DrawClear;
  DrawGrid;
  DrawLines;
  if Assigned(AfterPaint) then
    AfterPaint(self);
  paint.Repaint;
end;

end.
