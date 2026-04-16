import React, { useEffect, useMemo, useState } from "react";
import Highcharts from "highcharts/highmaps";
import HighchartsReact from "highcharts-react-official";
import { Button, Loading } from "@carbon/react";
import { Download, Renew } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";

const MAP_URL =
  "https://code.highcharts.com/mapdata/countries/id/id-all.topo.json";

type MapDatum = {
  code: string;
  name: string;
  value: number;
};

interface MapProps {
  data?: MapDatum[];
  disease?: string;
  threshold?: number;
  onDownload?: () => void;
}

const DEFAULT_DATA: MapDatum[] = [
  { code: "id-ji", name: "Jawa Timur", value: 55 },
  { code: "id-jt", name: "Jawa Tengah", value: 42 },
  { code: "id-jb", name: "Jawa Barat", value: 38 },
  { code: "id-jk", name: "DKI Jakarta", value: 65 },
  { code: "id-yo", name: "DI Yogyakarta", value: 28 },
  { code: "id-bt", name: "Banten", value: 45 },
];

const Map: React.FC<MapProps> = ({
  data = DEFAULT_DATA,
  disease = "Dengue",
  threshold = 50,
  onDownload,
}) => {
  const intl = useIntl();
  const [topology, setTopology] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    const loadMap = async () => {
      setLoading(true);
      setError(null);

      try {
        const response = await fetch(MAP_URL, {
          signal: controller.signal,
        });

        if (!response.ok) {
          throw new Error(
            `Failed to load Indonesia topology (${response.status})`,
          );
        }

        const topologyJson = await response.json();
        setTopology(topologyJson);
      } catch (fetchError) {
        if ((fetchError as Error).name !== "AbortError") {
          setError(
            intl.formatMessage({
              id: "dashboard.map.error",
              defaultMessage: "Unable to load the map.",
            }),
          );
        }
      } finally {
        setLoading(false);
      }
    };

    loadMap();

    return () => {
      controller.abort();
    };
  }, [intl]);

  const chartOptions = useMemo<Highcharts.Options>(
    () => ({
      chart: {
        backgroundColor: "transparent",
        spacing: [0, 0, 0, 0],
        height: 430,
      },
      title: {
        text: undefined,
      },
      subtitle: {
        text: undefined,
      },
      credits: {
        enabled: false,
      },
      exporting: {
        enabled: false,
      },
      legend: {
        enabled: false,
      },
      mapNavigation: {
        enabled: false,
      },
      colorAxis: {
        min: 0,
        max: threshold,
        minColor: "#EAF7F7",
        maxColor: "#0F6B6D",
      },
      tooltip: {
        useHTML: true,
        borderRadius: 10,
        shadow: false,
        backgroundColor: "#ffffff",
        formatter: function formatter(this: Highcharts.Point) {
          const point = this as Highcharts.Point & {
            value?: number;
            name?: string;
          };

          return `
            <div style="min-width: 160px; padding: 4px 2px;">
              <div style="font-weight: 600; color: #262626; margin-bottom: 6px;">${point.name ?? ""}</div>
              <div style="font-size: 14px; font-weight: 700; color: #0F8B8D; margin-bottom: 6px;">${disease}</div>
              <div style="display: grid; grid-template-columns: auto auto; gap: 4px 16px; font-size: 12px; color: #525252;">
                <span>${intl.formatMessage({
                  id: "dashboard.numberCases",
                  defaultMessage: "Number of Cases",
                })}</span>
                <span style="text-align: right; font-weight: 600; color: #262626;">${point.value ?? 0}</span>
                <span>${intl.formatMessage({
                  id: "dashboard.map.threshold",
                  defaultMessage: "Threshold",
                })}</span>
                <span style="text-align: right; font-weight: 600; color: #262626;">${threshold}</span>
              </div>
            </div>
          `;
        },
      },
      series: topology
        ? [
            {
              type: "map",
              name: disease,
              mapData: topology,
              data,
              joinBy: ["hc-key", "code"],
              allAreas: true,
              borderColor: "#ffffff",
              borderWidth: 1.2,
              nullColor: "#0F6B6D",
              states: {
                hover: {
                  color: "#BADA55",
                },
              },
              dataLabels: {
                enabled: false,
              },
            },
          ]
        : [],
    }),
    [data, disease, intl, threshold, topology],
  );

  const updatedTime = new Intl.DateTimeFormat("id-ID", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date());

  const updatedDate = new Intl.DateTimeFormat("id-ID", {
    weekday: "long",
    day: "2-digit",
    month: "long",
    year: "numeric",
  }).format(new Date());

  return (
    <section className="surveillance-map-container">
      <div className="surveillance-map-card">
        <div className="surveillance-map-header">
          <div className="surveillance-map-header-left">
            <Renew size={20} />
            <span>
              <FormattedMessage
                id="dashboard.map.updatedAt"
                defaultMessage="Terakhir di update, {time}"
                values={{ time: updatedTime }}
              />
            </span>
          </div>
          <div className="surveillance-map-header-right">
            <span>{updatedDate}</span>
            <Button
              kind="ghost"
              size="sm"
              hasIconOnly
              iconDescription={intl.formatMessage({
                id: "dashboard.map.download",
                defaultMessage: "Download",
              })}
              renderIcon={Download}
              onClick={onDownload}
            />
          </div>
        </div>

        <div className="surveillance-map-body">
          {loading ? (
            <div className="surveillance-map-state">
              <Loading
                withOverlay={false}
                description={intl.formatMessage({
                  id: "dashboard.map.loading",
                  defaultMessage: "Loading map...",
                })}
              />
            </div>
          ) : error ? (
            <div className="surveillance-map-state surveillance-map-error">
              {error}
            </div>
          ) : (
            <div className="surveillance-map-chart">
              <HighchartsReact
                highcharts={Highcharts}
                constructorType="mapChart"
                options={chartOptions}
              />
            </div>
          )}
        </div>
      </div>
    </section>
  );
};

export default Map;
