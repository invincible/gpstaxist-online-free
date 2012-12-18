program receivedata;

uses
  Forms,
  main in 'main.pas' {Form1},
  dcalc in 'dcalc.pas';

{$R *.res}

begin
  Application.Initialize;
  Application.CreateForm(TForm1, Form1);
  Application.Run;
end.
