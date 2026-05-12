param(
    [string]$ApiBase = "http://localhost:8080",
    [long]$EspectaculoId = 0,
    [string]$Artista = "Demo Cola - 3 entradas",
    [string]$ClientePrefix = "defensa",
    [switch]$FinalizarPrimerActivo
)

$ErrorActionPreference = "Stop"

function Get-DemoEspectaculoId {
    param([string]$ApiBase, [string]$Artista)

    $url = "$ApiBase/busqueda/getEspectaculos?artista=$([uri]::EscapeDataString($Artista))"
    $resultados = Invoke-RestMethod -Method Get -Uri $url

    if (-not $resultados -or $resultados.Count -eq 0) {
        throw "No encuentro el espectaculo '$Artista'. Ejecuta antes scripts/cola-demo.mysql.sql en MySQL."
    }

    return [long]$resultados[0].id
}

if ($EspectaculoId -le 0) {
    $EspectaculoId = Get-DemoEspectaculoId -ApiBase $ApiBase -Artista $Artista
}

Write-Host "Espectaculo demo: $EspectaculoId"
Write-Host "Entrando 5 clientes anonimos en cola..."

$turnos = @()
foreach ($numero in 1..5) {
    $clienteId = "$ClientePrefix-$numero"
    $url = "$ApiBase/cola/entrar?idEspectaculo=$EspectaculoId&clienteId=$clienteId"
    $turno = Invoke-RestMethod -Method Post -Uri $url
    $turnos += [pscustomobject]@{
        Cliente = $clienteId
        Turno = $turno.idTurno
        Estado = $turno.estado
        Posicion = $turno.posicion
        PuedePrerreservar = $turno.puedePrerreservar
        Mensaje = $turno.mensaje
    }
}

$turnos | Format-Table -AutoSize

if ($FinalizarPrimerActivo) {
    $activo = $turnos | Where-Object { $_.Estado -eq "ACTIVO" } | Select-Object -First 1
    if ($activo) {
        Write-Host "Finalizando el primer turno activo: $($activo.Cliente)"
        Invoke-RestMethod -Method Post -Uri "$ApiBase/cola/finalizar/$($activo.Turno)?clienteId=$($activo.Cliente)" | Out-Null
    }
}

Write-Host "Estado actualizado:"
$estadoActual = @()
foreach ($turno in $turnos) {
    $estado = Invoke-RestMethod -Method Get -Uri "$ApiBase/cola/estado/$($turno.Turno)?clienteId=$($turno.Cliente)"
    $estadoActual += [pscustomobject]@{
        Cliente = $turno.Cliente
        Turno = $estado.idTurno
        Estado = $estado.estado
        Posicion = $estado.posicion
        PuedePrerreservar = $estado.puedePrerreservar
        Mensaje = $estado.mensaje
    }
}

$estadoActual | Format-Table -AutoSize

Write-Host "Para avanzar la cola, vuelve a ejecutar con -FinalizarPrimerActivo."
