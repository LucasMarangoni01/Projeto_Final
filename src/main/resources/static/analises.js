(() => {
    "use strict";

    const palette = {
        blue: "#4f8edc",
        blueLight: "#8db9f4",
        orange: "#e58a3a",
        graphite: "#9ca3af",
        cyan: "#39b8d1",
        green: "#65b88a",
        red: "#d86b70",
        purple: "#9a83cf",
        yellow: "#d9b45d",
        teal: "#4d9f9a"
    };

    const colors = [
        palette.blue,
        palette.orange,
        palette.graphite,
        palette.cyan,
        palette.green,
        palette.red,
        palette.purple,
        palette.yellow
    ];

    const soma = valores => (valores || [])
        .reduce((total, valor) => total + (Number(valor) || 0), 0);

    const semDados = (canvasId, emptyId, possuiDados) => {
        const canvas = document.getElementById(canvasId);
        const empty = document.getElementById(emptyId);

        if (!canvas || !empty) {
            return null;
        }

        if (!possuiDados) {
            canvas.hidden = true;
            empty.hidden = false;
            return null;
        }

        canvas.hidden = false;
        empty.hidden = true;

        if (typeof Chart === "undefined") {
            canvas.hidden = true;
            empty.hidden = false;
            empty.querySelector("strong").textContent = "Biblioteca de gráficos indisponível";
            empty.querySelector("span").textContent =
                "Verifique a conexão com a internet e atualize a página.";
            return null;
        }

        return canvas;
    };

    const baseOptions = {
        responsive: true,
        maintainAspectRatio: false,
        animation: {
            duration: 650,
            easing: "easeOutQuart"
        },
        plugins: {
            legend: {
                position: "bottom",
                labels: {
                    color: "#d6e2f4",
                    usePointStyle: true,
                    pointStyle: "line",
                    boxWidth: 22,
                    boxHeight: 8,
                    padding: 16,
                    font: {
                        family: "'IBM Plex Sans', 'Segoe UI', sans-serif",
                        size: 10,
                        weight: "600"
                    }
                }
            },
            tooltip: {
                backgroundColor: "#081a3b",
                titleColor: "#ffffff",
                bodyColor: "#dbeafe",
                borderColor: "rgba(255,255,255,.14)",
                borderWidth: 1,
                padding: 11,
                displayColors: true,
                titleFont: {
                    family: "'IBM Plex Sans', 'Segoe UI', sans-serif",
                    weight: "700"
                },
                bodyFont: {
                    family: "'IBM Plex Sans', 'Segoe UI', sans-serif",
                    size: 11
                }
            }
        },
        scales: {
            x: {
                grid: {
                    color: "rgba(210,225,248,.08)"
                },
                ticks: {
                    color: "#afc2df",
                    font: {
                        family: "'IBM Plex Sans', 'Segoe UI', sans-serif",
                        size: 10
                    }
                },
                border: {
                    color: "rgba(210,225,248,.14)"
                }
            },
            y: {
                beginAtZero: true,
                grid: {
                    color: "rgba(210,225,248,.10)"
                },
                ticks: {
                    precision: 0,
                    color: "#afc2df",
                    font: {
                        family: "'IBM Plex Sans', 'Segoe UI', sans-serif",
                        size: 10
                    }
                },
                border: {
                    display: false
                }
            }
        }
    };

    const linha = (label, dados, color) => ({
        label,
        data: dados,
        borderColor: color,
        backgroundColor: color,
        pointBackgroundColor: color,
        pointBorderColor: "#0b2453",
        pointBorderWidth: 2,
        pointRadius: 3.5,
        pointHoverRadius: 5,
        borderWidth: 2.5,
        tension: 0.28,
        fill: false
    });

    const criarGrafico = (canvas, tipo, data, options = {}) => {
        if (!canvas) {
            return;
        }

        new Chart(canvas, {
            type: tipo,
            data,
            options: {
                ...baseOptions,
                ...options,
                plugins: {
                    ...baseOptions.plugins,
                    ...(options.plugins || {})
                }
            }
        });
    };

    document.addEventListener("DOMContentLoaded", () => {
        const dados = window.dadosAnalise || {};
        const meses = dados.meses || [];
        const incidentes = dados.incidentes || [];
        const manutencoes = dados.manutencoes || [];
        const concluidas = dados.concluidas || [];
        const emAndamento = dados.emAndamento || [];

        // 1. Colunas: quantidade histórica de incidentes por mês.
        const canvasColunas = semDados(
            "graficoColunas",
            "vazioColunas",
            soma(incidentes) > 0
        );

        if (canvasColunas) {
            criarGrafico(canvasColunas, "bar", {
                labels: meses,
                datasets: [{
                    label: "Incidentes registrados",
                    data: incidentes,
                    backgroundColor: "rgba(79,142,220,.82)",
                    hoverBackgroundColor: palette.blueLight,
                    borderColor: palette.blue,
                    borderWidth: 1,
                    borderRadius: 5,
                    maxBarThickness: 30
                }]
            }, {
                plugins: {
                    legend: { display: false }
                }
            });
        }

        // 2. Linha com as três séries pedidas: incidentes, manutenções e concluídas.
        const canvasLinhas = semDados(
            "graficoLinhas",
            "vazioLinhas",
            soma(incidentes) + soma(manutencoes) > 0
        );

        if (canvasLinhas) {
            criarGrafico(canvasLinhas, "line", {
                labels: meses,
                datasets: [
                    linha("Incidentes", incidentes, palette.blueLight),
                    linha("Manutenções registradas", manutencoes, palette.orange),
                    linha("Manutenções concluídas", concluidas, palette.graphite)
                ]
            }, {
                interaction: {
                    mode: "index",
                    intersect: false
                }
            });
        }

        // 3. Pizza: status real das atividades de manutenção no ano selecionado.
        const statusValores = dados.statusValores || [];
        const canvasPizza = semDados(
            "graficoPizza",
            "vazioPizza",
            soma(statusValores) > 0
        );

        if (canvasPizza) {
            criarGrafico(canvasPizza, "pie", {
                labels: dados.statusLabels || [],
                datasets: [{
                    data: statusValores,
                    backgroundColor: colors.slice(0, statusValores.length),
                    borderColor: "#10275a",
                    borderWidth: 2,
                    hoverOffset: 7
                }]
            }, {
                scales: {},
                plugins: {
                    legend: {
                        position: "bottom"
                    }
                }
            });
        }

        // 4. Rosca: classificação de risco salva nas manutenções do período.
        const riscoValores = dados.riscoValores || [];
        const canvasRosca = semDados(
            "graficoRosca",
            "vazioRosca",
            soma(riscoValores) > 0
        );

        if (canvasRosca) {
            criarGrafico(canvasRosca, "doughnut", {
                labels: dados.riscoLabels || [],
                datasets: [{
                    data: riscoValores,
                    backgroundColor: [
                        palette.green,
                        palette.yellow,
                        palette.orange,
                        palette.red
                    ],
                    borderColor: "#10275a",
                    borderWidth: 2,
                    hoverOffset: 7
                }]
            }, {
                cutout: "58%",
                plugins: {
                    legend: {
                        position: "bottom"
                    }
                }
            });
        }

        // 5. Barras horizontais: localizações ordenadas pela quantidade de incidentes.
        const localizacaoValores = dados.localizacaoValores || [];
        const canvasBarras = semDados(
            "graficoBarras",
            "vazioBarras",
            soma(localizacaoValores) > 0
        );

        if (canvasBarras) {
            criarGrafico(canvasBarras, "bar", {
                labels: dados.localizacaoLabels || [],
                datasets: [{
                    label: "Incidentes registrados",
                    data: localizacaoValores,
                    backgroundColor: "rgba(101,184,138,.82)",
                    hoverBackgroundColor: palette.green,
                    borderRadius: 5,
                    borderSkipped: false,
                    maxBarThickness: 25
                }]
            }, {
                indexAxis: "y",
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    x: {
                        beginAtZero: true,
                        ticks: {
                            precision: 0,
                            color: "#afc2df"
                        },
                        grid: {
                            color: "rgba(210,225,248,.10)"
                        }
                    },
                    y: {
                        grid: { display: false },
                        ticks: {
                            color: "#d6e2f4",
                            font: { size: 10 }
                        }
                    }
                }
            });
        }

        // 6. Radar: mesmos dados mensais reais para comparação de perfil operacional.
        const canvasRadar = semDados(
            "graficoRadar",
            "vazioRadar",
            soma(incidentes) + soma(manutencoes) > 0
        );

        if (canvasRadar) {
            criarGrafico(canvasRadar, "radar", {
                labels: dados.radarLabels || meses,
                datasets: [
                    {
                        label: "Incidentes",
                        data: incidentes,
                        borderColor: palette.blueLight,
                        backgroundColor: "rgba(141,185,244,.12)",
                        pointBackgroundColor: palette.blueLight,
                        borderWidth: 2
                    },
                    {
                        label: "Manutenções registradas",
                        data: manutencoes,
                        borderColor: palette.orange,
                        backgroundColor: "rgba(229,138,58,.08)",
                        pointBackgroundColor: palette.orange,
                        borderWidth: 2
                    },
                    {
                        label: "Em andamento",
                        data: emAndamento,
                        borderColor: palette.graphite,
                        backgroundColor: "rgba(156,163,175,.06)",
                        pointBackgroundColor: palette.graphite,
                        borderWidth: 2
                    }
                ]
            }, {
                scales: {
                    r: {
                        beginAtZero: true,
                        angleLines: {
                            color: "rgba(210,225,248,.13)"
                        },
                        grid: {
                            color: "rgba(210,225,248,.13)"
                        },
                        pointLabels: {
                            color: "#d6e2f4",
                            font: {
                                family: "'IBM Plex Sans', 'Segoe UI', sans-serif",
                                size: 10
                            }
                        },
                        ticks: {
                            precision: 0,
                            color: "#afc2df",
                            backdropColor: "rgba(8,26,59,.75)",
                            showLabelBackdrop: true
                        }
                    }
                }
            });
        }

        // 7. Rosca: fotografia atual do estado cadastrado de todas as flanges.
        const flangeValores = dados.flangesValores || [];
        const canvasFlanges = semDados(
            "graficoFlanges",
            "vazioFlanges",
            soma(flangeValores) > 0
        );

        if (canvasFlanges) {
            criarGrafico(canvasFlanges, "doughnut", {
                labels: dados.flangesLabels || [],
                datasets: [{
                    data: flangeValores,
                    backgroundColor: [
                        palette.green,
                        palette.yellow,
                        palette.red,
                        palette.cyan
                    ],
                    borderColor: "#10275a",
                    borderWidth: 2,
                    hoverOffset: 7
                }]
            }, {
                cutout: "58%",
                plugins: {
                    legend: {
                        position: "bottom"
                    }
                }
            });
        }
    });
})();
