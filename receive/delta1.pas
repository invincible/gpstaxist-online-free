unit delta1;

interface

uses
  city10, Classes;

type
  TDeltaPoint = class
  public
    x,y: Float;
  end;

  TDeltaBound = record
    x1,y1,x2,y2: Float;
  end;

  TDeltaLink = class
  private
    dist__: Float;
    bound__: TDeltaBound;
    ta__: Float;
    tb__: Float;
  public
    a,b: TDeltaPoint;
    function dist: Float;   //calculated value
    function dist_: Float;  //proxy value
    function bound: TDeltaBound;
    function bound_: TDeltaBound;
    function ta: Float;
    function ta_: Float;
    function tb: Float;
    function tb_: Float;
  end;

  TDeltaPointList = class (TList)
  private
    function getItem(index: Integer): TDeltaPoint;
    procedure setItem(index: Integer; value: TDeltaPoint);
  public
    function Add: TDeltaPoint;  overload;
//    function Add(n: TDeltaPoint): Integer;  overload;
    function Add(x,y: Float): TDeltaPoint;  overload;
    procedure Delete(index: Integer);  overload;
    procedure Delete(n: TDeltaPoint);  overload;
    property Items[index: Integer]: TDeltaPoint  read getItem  write setItem;  default;
    procedure Clear;  override;
    destructor Destroy;  override;
  end;

  TDeltaLinkList = class (TList)
  private
    function getItem(index: Integer): TDeltaLink;
    procedure setItem(index: Integer; value: TDeltaLink);
  public
    function Add: TDeltaLink;  overload;
//    function Add(n: TDeltaLink): Integer;  overload;
    function Add(a,b: TDeltaPoint): TDeltaLink;  overload;
    procedure Delete(index: Integer);  overload;
    procedure Delete(n: TDeltaLink);  overload;
    property Items[index: Integer]: TDeltaLink  read getItem  write setItem;  default;
    procedure Clear;  override;
    destructor Destroy;  override;
  end;

  TDeltaGraph = class
  private
    procedure LoadCity(list: TCityList);
    procedure Weld(f: Float);
    procedure DeleteNil;
    procedure Break;
  public
    Points: TDeltaPointList;
    Links: TDeltaLinkList;
    constructor Create;
    destructor Destroy;  override;
    procedure Make(list: TCityList);
  end;

implementation

uses
  Math;

const
  z0 = 1/10000;

function DistanceFast(a,b: TDeltaPoint): Float;
begin
  Result:=Abs(a.x-b.x)+Abs(a.y-b.y);
end;

function Distance(a,b: TDeltaPoint): Float;
begin
  Result:=Sqrt(Sqr(a.x-b.x)+Sqr(a.y-b.y));
end;

// ------------- TDeltaLink -------------

function TDeltaLink.dist: Float;
begin
  dist__:=Distance(a, b);
  Result:=dist__;
end;

function TDeltaLink.dist_: Float;
begin
  if dist__<>0 then
    Result:=dist__
  else
    Result:=dist;
end;

function TDeltaLink.bound: TDeltaBound;
begin
  bound__.x1:=Min(a.x, b.x);
  bound__.x2:=Max(a.x, b.x);
  bound__.y1:=Min(a.y, b.y);
  bound__.y2:=Max(a.y, b.y);
  Result:=bound__;
end;

function TDeltaLink.bound_: TDeltaBound;
begin
  if bound__.x1<>0 then
    Result:=bound__
  else
    Result:=bound;
end;

function TDeltaLink.ta: Float;
begin
  if Abs(a.x-b.x)<z0 then
    ta__:=1/z0
  else
    ta__:=(a.y-b.y)/(a.x-b.x);
  Result:=ta__;
end;

function TDeltaLink.ta_: Float;
begin
  if ta__<>0 then
    Result:=ta__
  else
    Result:=ta;
end;

function TDeltaLink.tb: Float;
begin
  tb__:=a.y-ta*a.x;
  Result:=tb__;
end;

function TDeltaLink.tb_: Float;
begin
  if tb__<>0 then
    Result:=tb__
  else
    Result:=tb;
end;

// ------------- TDeltaPointList -------------

function TDeltaPointList.getItem(index: Integer): TDeltaPoint;
begin
  Result:=inherited Items[index];
end;

procedure TDeltaPointList.setItem(index: Integer; value: TDeltaPoint);
begin
  inherited Items[index]:=value;
end;

function TDeltaPointList.Add: TDeltaPoint;
begin
  Result:=TDeltaPoint.Create;
  inherited Add(Result);
end;

function TDeltaPointList.Add(x,y: Float): TDeltaPoint;
begin
  Result:=Add;
  Result.x:=x;
  Result.y:=y;
end;

procedure TDeltaPointList.Delete(index: Integer);
begin
  Items[index].Free;
  inherited;
end;

procedure TDeltaPointList.Delete(n: TDeltaPoint);
begin
  inherited Remove(n);
  n.Free;
end;

procedure TDeltaPointList.Clear;
var
  i: Integer;
begin
  for i:=Count-1 downto 0 do
    Items[i].Free;
  inherited;
end;

destructor TDeltaPointList.Destroy;
begin
  Clear;
  inherited;
end;

// ------------- TDeltaLinkList -------------

function TDeltaLinkList.getItem(index: Integer): TDeltaLink;
begin
  Result:=inherited Items[index];
end;

procedure TDeltaLinkList.setItem(index: Integer; value: TDeltaLink);
begin
  inherited Items[index]:=value;
end;

function TDeltaLinkList.Add: TDeltaLink;
begin
  Result:=TDeltaLink.Create;
  inherited Add(Result);
end;

function TDeltaLinkList.Add(a,b: TDeltaPoint): TDeltaLink;
begin
  Result:=Add;
  Result.a:=a;
  Result.b:=b;
end;

procedure TDeltaLinkList.Delete(index: Integer);
begin
  Items[index].Free;
  inherited;
end;

procedure TDeltaLinkList.Delete(n: TDeltaLink);
begin
  inherited Remove(n);
  n.Free;
end;

procedure TDeltaLinkList.Clear;
var
  i: Integer;
begin
  for i:=Count-1 downto 0 do
    Items[i].Free;
  inherited;
end;

destructor TDeltaLinkList.Destroy;
begin
  Clear;
  inherited;
end;

// ------------- TDelta -------------

constructor TDeltaGraph.Create;
begin
  inherited;
  Points:=TDeltaPointList.Create;
  Links:=TDeltaLinkList.Create;
end;

destructor TDeltaGraph.Destroy;
begin
  Links.Free;
  Points.Free;
  inherited;
end;

procedure TDeltaGraph.LoadCity;

  procedure LoadLine(l: TLine);
  var
    i: Integer;
    p,p_: TDeltaPoint;
  begin
    p_:=nil;
    for i:=0 to l.Vertexes.Count-1 do begin
      p:=Points.Add(l.Vertexes[i].x, l.Vertexes[i].y);
      if i>=1 then
        Links.Add(p, p_);
      p_:=p;
    end;
  end;

var
  i: Integer;
begin
  for i:=0 to list.Count-1 do
    if list[i] is TLine then
      LoadLine(list[i] as TLine);
end;

procedure TDeltaGraph.DeleteNil;
var
  i: Integer;
begin
  for i:=Points.Count-1 downto 0 do
    if Points[i]=nil then
      Points.Delete(i);
  for i:=Links.Count-1 downto 0 do
    if Links[i]=nil then
      Links.Delete(i);
end;

procedure TDeltaGraph.Weld(f: Float);

  function WeldPoints(p, q: TDeltaPoint): TDeltaPoint;
  var
    s: TDeltaPoint;
    i: Integer;
    v: Boolean;
  begin
    s:=Points.Add((p.x+q.x)/2, (p.y+q.y)/2);
    Result:=s;
    for i:=0 to Links.Count-1 do
      if Links[i]<>nil then begin
        v:=true;
        if Links[i].a=p then  Links.Add(s, Links[i].b)  else
        if Links[i].a=q then  Links.Add(s, Links[i].b)  else
        if Links[i].b=p then  Links.Add(Links[i].a, s)  else
        if Links[i].b=q then  Links.Add(Links[i].a, s)  else
          v:=false;
        if v then begin
          Links[i].Free;  Links[i]:=nil;
        end;
      end;
  end;

var
  i,j: Integer;
begin
  for i:=0 to Points.Count-1 do
    for j:=0 to Points.Count-1 do
      if i<>j then
        if (Points[i]<>nil) and (Points[j]<>nil) then
          if DistanceFast(Points[i], Points[j])<=f*2 then
            if Distance(Points[i], Points[j])<f then begin
              WeldPoints(Points[i], Points[j]);
              Points[i].Free;  Points[i]:=nil;
              Points[j].Free;  Points[j]:=nil;
            end;
  DeleteNil;
end;

procedure TDeltaGraph.Break;

  function CheckLines(m1,m2: TDeltaLink): Boolean;
  var
    o1,o2: TDeltaBound;

    function BreakLines: Boolean;
    var
      c: TDeltaPoint;
      a1,a2,b1,b2: Float;
    begin
      Result:=false;
      c:=TDeltaPoint.Create;
      try
        a1:=m1.ta_;  b1:=m1.tb_;
        a2:=m2.ta_;  b2:=m2.tb_;

        c.x:=(b2-b1)/(a1-a2);
        c.y:=(a1*b2-a2*b1)/(a1-a2);
        if
          (c.x>o1.x1) and (c.x<o1.x2) and
          (c.y>o1.y1) and (c.y<o1.y2) and
          (c.x>o2.x1) and (c.x<o2.x2) and
          (c.y>o2.y1) and (c.y<o2.y2)
        then begin
          Result:=true;
          Points.Add(c);
          Links.Add(m1.a, c);  Links.Add(m1.b, c);
          Links.Add(m2.a, c);  Links.Add(m2.b, c);
        end;
      except
      end;
      if not Result then
        c.Free;
    end;

  begin
    if (m1.a=m2.a) or (m1.a=m2.b) or (m1.b=m2.a) or (m1.b=m2.b) then
      Result:=false  //not crosses because one common point
    else begin
      o1:=m1.bound_;
      o2:=m2.bound_;
      if
        (o1.x1>o2.x2) or (o1.x2<o2.x1) or
        (o1.y1>o2.y2) or (o1.y2<o2.y1)
      then
        Result:=false  //not crosses because checking bound boxes
      else
        Result:=BreakLines;
    end;
  end;

var
  i,j: Integer;
begin
  i:=0;
  if i<Links.Count then
    repeat
      j:=i+1;
      if j<Links.Count then
        repeat
//          if i<>j then
            if (Links[i]<>nil) and (Links[j]<>nil) then
              if CheckLines(Links[i], Links[j]) then begin
                Links[i].Free;  Links[i]:=nil;
                Links[j].Free;  Links[j]:=nil;
              end;
          Inc(j);
        until j>=Links.Count;
      Inc(i);
    until i>=Links.Count;
  DeleteNil;
end;

procedure TDeltaGraph.Make(list: TCityList);
begin
  Points.Clear;
  Links.Clear;
  LoadCity(list);
  Weld(0.5);
  Break;
  Weld(1.5);
end;

end.
