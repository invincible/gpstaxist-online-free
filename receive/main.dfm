object Form1: TForm1
  Left = 293
  Top = 228
  BorderStyle = bsSingle
  Caption = 'Receive form'
  ClientHeight = 298
  ClientWidth = 349
  Color = clBtnFace
  Font.Charset = DEFAULT_CHARSET
  Font.Color = clWindowText
  Font.Height = -11
  Font.Name = 'MS Sans Serif'
  Font.Style = []
  OldCreateOrder = False
  Position = poScreenCenter
  WindowState = wsMinimized
  OnCreate = FormCreate
  OnDestroy = FormDestroy
  PixelsPerInch = 96
  TextHeight = 13
  object Label4: TLabel
    Left = 24
    Top = 16
    Width = 49
    Height = 13
    Caption = 'Listen port'
  end
  object Label1: TLabel
    Left = 24
    Top = 88
    Width = 67
    Height = 13
    Caption = 'Incoming data'
  end
  object EditPort: TEdit
    Left = 24
    Top = 32
    Width = 121
    Height = 21
    TabOrder = 0
    Text = '33401'
    OnChange = EditPortChange
  end
  object MemoData: TMemo
    Left = 24
    Top = 104
    Width = 193
    Height = 177
    TabOrder = 1
  end
  object Button1: TButton
    Left = 240
    Top = 16
    Width = 99
    Height = 25
    Caption = 'Create Streets.txt'
    TabOrder = 2
    OnClick = Button1Click
  end
  object Edit1: TEdit
    Left = 216
    Top = 48
    Width = 121
    Height = 21
    TabOrder = 3
    Text = 'Edit1'
  end
  object TcpServer1: TTcpServer
    OnAccept = TcpServer1Accept
    Left = 160
    Top = 32
  end
end
