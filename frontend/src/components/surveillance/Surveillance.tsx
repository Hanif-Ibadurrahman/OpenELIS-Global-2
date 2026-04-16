import React from "react";
import Highcharts from "highcharts/highmaps";
import HighchartsReact from "highcharts-react-official";
import {
  Grid,
  Column,
  Tile,
  Link,
  Loading,
  ClickableTile,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Pagination,
  Dropdown,
  DatePicker,
  DatePickerInput,
  Tab,
  Tabs,
  TabList,
  Button,
} from "@carbon/react";
import { Minimize, Maximize, ArrowLeft, ArrowRight } from "@carbon/react/icons";
import { Copy } from "@carbon/icons-react";
import { useState, useEffect, useRef, useContext, useMemo } from "react";
import {
  getFromOpenElisServer,
  convertAlphaNumLabNumForDisplay,
  hasRole,
} from "../utils/Utils.js";
import { FormattedMessage, useIntl } from "react-intl";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import Map from "./Map";
import "./Surveillance.css";

interface SurveillanceProps {}

interface Tile {
  title: string | JSX.Element;
  subTitle: string | JSX.Element;
  type: MetricType;
  value: number;
  id?: number;
}
type MetricType =
  | "ORDERS_IN_PROGRESS"
  | "ORDERS_READY_FOR_VALIDATION"
  | "ORDERS_COMPLETED_TODAY"
  | "ORDERS_PATIALLY_COMPLETED_TODAY"
  | "ORDERS_ENTERED_BY_USER_TODAY"
  | "ORDERS_REJECTED_TODAY"
  | "UN_PRINTED_RESULTS"
  | "INCOMING_ORDERS"
  | "AVERAGE_TURN_AROUND_TIME"
  | "DELAYED_TURN_AROUND"
  | "ORDERS_FOR_USER";

interface UserSessionDetails {
  userSessionDetails: any;
}

interface Notification {
  notificationVisible: any;
  setNotificationVisible: any;
  addNotification: any;
}

interface DiseaseOption {
  id: string;
  label: string;
  icdCode: string;
}

interface AggregateRow {
  icdCode: string;
  name: string;
  total: number;
  region?: string;
}

const DEFAULT_DISEASE_OPTIONS: DiseaseOption[] = [
  { id: "A90", label: "Dengue", icdCode: "A90" },
  { id: "A15", label: "Tuberculosis", icdCode: "A15" },
  { id: "U07.1", label: "COVID-19", icdCode: "U07.1" },
];

const REGION_CODE_MAP: Record<string, string> = {
  "dki jakarta": "id-jk",
  jakarta: "id-jk",
  "jawa timur": "id-ji",
  "east java": "id-ji",
  "jawa tengah": "id-jt",
  "central java": "id-jt",
  "jawa barat": "id-jb",
  "west java": "id-jb",
  banten: "id-bt",
  "di yogyakarta": "id-yo",
  yogyakarta: "id-yo",
};

const Surveillance: React.FC<SurveillanceProps> = () => {
  const intl = useIntl();

  const [counts, setCounts] = useState({
    ordersInProgress: 0,
    ordersReadyForValidation: 0,
    ordersCompletedToday: 0,
    patiallyCompletedToday: 0,
    orderEnterdByUserToday: 0,
    ordersRejectedToday: 0,
    unPritendResults: 0,
    incomigOrders: 0,
    averageTurnAroudTime: 0,
    delayedTurnAround: 0,
  });

  const [timeMetrics, setTimeMetrics] = useState({
    receptionToResult: 0,
    resultToValidation: 0,
    receptionToValidation: 0,
  });

  const [data, setData] = useState([]);
  const [testSections, setTestSections] = useState([]);
  const [selectedTestSection, setSelectedTestSection] = useState("");
  const [loading, setLoading] = useState(true);
  const componentMounted = useRef(true);
  const [diseaseFilter, setDiseaseFilter] = useState(null);
  const [diseaseOptions, setDiseaseOptions] = useState<DiseaseOption[]>(
    DEFAULT_DISEASE_OPTIONS,
  );
  const [statusFilter, setStatusFilter] = useState(null);
  const [dateRange, setDateRange] = useState({ start: null, end: null });
  const [nationalTotals, setNationalTotals] = useState<AggregateRow[]>([]);
  const [regionalTotals, setRegionalTotals] = useState<AggregateRow[]>([]);
  const [surveillanceDataLoading, setSurveillanceDataLoading] = useState(false);

  const [trendData, setTrendData] = useState([
    { label: "Apr 10", value: 12 },
    { label: "Apr 14", value: 55 },
    { label: "Apr 16", value: 25 },
    { label: "Apr 17", value: 43 },
    { label: "Apr 19", value: 40 },
    { label: "Apr 20", value: 50 },
    { label: "Apr 21", value: 30 },
    { label: "Apr 22", value: 38 },
    { label: "Apr 23", value: 18 },
    { label: "Apr 24", value: 32 },
  ]);
  const [disease, setDisease] = useState("Dengue");
  const [threshold, setThreshold] = useState(50);

  const selectedDiseaseOption = useMemo(
    () => diseaseOptions.find((item) => item.id === diseaseFilter) || null,
    [diseaseFilter, diseaseOptions],
  );

  const trendChartOptions = useMemo<Highcharts.Options>(
    () => ({
      chart: {
        type: "column",
        backgroundColor: "#eef8f8",
        spacing: [16, 12, 20, 12],
        height: 340,
      },
      title: {
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
      xAxis: {
        categories: trendData.map((item) => item.label),
        lineColor: "rgba(15, 107, 109, 0.18)",
        tickColor: "rgba(15, 107, 109, 0.18)",
        labels: {
          style: {
            color: "#525252",
            fontSize: "12px",
          },
        },
      },
      yAxis: {
        min: 0,
        title: {
          text: undefined,
        },
        gridLineColor: "rgba(15, 107, 109, 0.18)",
        labels: {
          style: {
            color: "#525252",
            fontSize: "12px",
          },
        },
        plotLines: [
          {
            value: threshold,
            color: "#FF5A52",
            dashStyle: "ShortDash",
            width: 2,
            label: {
              text: intl.formatMessage({
                id: "dashboard.map.threshold",
                defaultMessage: "Threshold",
              }),
              style: {
                color: "#FF5A52",
                fontWeight: "600",
              },
              align: "right",
              x: -8,
              y: -6,
            },
          },
        ],
      },
      tooltip: {
        useHTML: true,
        backgroundColor: "#FFFFFF",
        borderRadius: 10,
        shadow: false,
        formatter: function formatter(this: Highcharts.Point) {
          const point = this as Highcharts.Point & { y?: number };

          return `
            <div style="min-width: 150px; padding: 4px 2px;">
              <div style="font-weight: 600; color: #262626; margin-bottom: 6px;">${this.category ?? ""}</div>
              <div style="font-size: 14px; font-weight: 700; color: #0F8B8D; margin-bottom: 6px;">${disease}</div>
              <div style="display: grid; grid-template-columns: auto auto; gap: 4px 16px; font-size: 12px; color: #525252;">
                <span>${intl.formatMessage({
                  id: "dashboard.numberCases",
                  defaultMessage: "Number of Cases",
                })}</span>
                <span style="text-align: right; font-weight: 600; color: #262626;">${point.y ?? 0}</span>
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
      plotOptions: {
        column: {
          borderWidth: 0,
          borderRadius: 0,
          pointPadding: 0.08,
          groupPadding: 0.12,
          dataLabels: {
            enabled: true,
            inside: true,
            crop: false,
            overflow: "allow",
            style: {
              color: "#FFFFFF",
              textOutline: "none",
              fontWeight: "600",
              fontSize: "12px",
            },
          },
        },
      },
      series: [
        {
          type: "column",
          name: disease,
          data: trendData.map((item) => ({
            y: item.value,
            color: item.value >= threshold ? "#20B8B4" : "#1FB2AD",
          })),
        },
      ],
    }),
    [disease, intl, threshold, trendData],
  );

  const STATUS_OPTIONS = [
    {
      id: "active",
      label: intl.formatMessage({
        id: "surveillance.status.active",
        defaultMessage: "Aktif",
      }),
    },
    {
      id: "inactive",
      label: intl.formatMessage({
        id: "surveillance.status.inactive",
        defaultMessage: "Non aktif",
      }),
    },
  ];

  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(100);
  const [selectedTile, setSelectedTile] = useState<Tile>(null);
  const [nextPage, setNextPage] = useState(null);
  const [previousPage, setPreviousPage] = useState(null);
  const [pagination, setPagination] = useState(false);
  const [currentApiPage, setCurrentApiPage] = useState(null);
  const [totalApiPages, setTotalApiPages] = useState(null);
  const { userSessionDetails } = useContext(
    UserSessionDetailsContext,
  ) as UserSessionDetails;
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext) as Notification;
  const [notifications, setNotifications] = useState([]);
  const [notificationsLoading, setNotificationsLoading] = useState(false);

  const PAGE_SIZES = [25, 50, 100];

  useEffect(() => {
    setNextPage(null);
    setPreviousPage(null);
    setPagination(false);
  }, []);

  useEffect(() => {
    getFromOpenElisServer("/rest/home-dashboard/metrics", loadCount);

    return () => {
      // This code runs when component is unmounted
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    getFromOpenElisServer("/rest/surveillance/icd-codes", (response) => {
      if (
        !componentMounted.current ||
        !Array.isArray(response) ||
        !response.length
      ) {
        return;
      }

      const options = response.map((item) => ({
        id: item.code,
        label: item.title,
        icdCode: item.code,
      }));

      setDiseaseOptions(options);
      setDiseaseFilter(
        (currentValue) => currentValue || options[0]?.id || null,
      );
      setDisease(
        (currentValue) => currentValue || options[0]?.label || currentValue,
      );
    });
  }, []);

  useEffect(() => {
    if (selectedDiseaseOption?.label) {
      setDisease(selectedDiseaseOption.label);
    }
  }, [selectedDiseaseOption]);

  useEffect(() => {
    if (nationalTotals.length) {
      setTrendData(
        nationalTotals.slice(0, 10).map((item) => ({
          label: item.name,
          value: item.total,
        })),
      );
      setThreshold(Math.max(50, ...nationalTotals.map((item) => item.total)));
      return;
    }

    setTrendData([]);
    setThreshold(50);
  }, [nationalTotals]);

  useEffect(() => {
    loadDiseaseAggregates();
  }, [diseaseFilter]);

  useEffect(() => {
    if (selectedTile != null) {
      setNextPage(null);
      setPreviousPage(null);
      setPagination(false);
      setLoading(true);
      if (selectedTile.type == "AVERAGE_TURN_AROUND_TIME") {
        getFromOpenElisServer(
          "/rest/home-dashboard/turn-around-time-metrics",
          loadTimeMetrics,
        );
      } else if (selectedTile.type == "ORDERS_FOR_USER") {
        getFromOpenElisServer(
          "/rest/home-dashboard/" +
            selectedTile.type +
            "?systemUserId=" +
            selectedTile.id,
          loadData,
        );
      } else {
        getFromOpenElisServer(
          "/rest/home-dashboard/" + selectedTile.type,
          loadData,
        );
      }
    }

    return () => {
      // This code runs when component is unmounted
      componentMounted.current = false;
    };
  }, [selectedTile]);

  useEffect(() => {
    getFromOpenElisServer(
      "/rest/user-test-sections/ALL",
      (fetchedTestSections) => {
        fetchTestSections(fetchedTestSections);
      },
    );
    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    loadNotifications();
    // Keep componentMounted safety flag for other hooks
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const fetchTestSections = (res) => {
    setTestSections(res);
    hasRole(userSessionDetails, "Global Administrator")
      ? setSelectedTestSection("all")
      : setSelectedTestSection(res[0]?.id);
  };

  const loadNextResultsPage = () => {
    setLoading(true);
    getFromOpenElisServer(
      "/rest/home-dashboard/" + selectedTile.type + "?page=" + nextPage,
      loadData,
    );
  };

  const loadPreviousResultsPage = () => {
    setLoading(true);
    getFromOpenElisServer(
      "/rest/home-dashboard/" + selectedTile.type + "?page=" + previousPage,
      loadData,
    );
  };

  const loadCount = (data) => {
    if (componentMounted.current) {
      setCounts(data);
      setLoading(false);
    }
  };

  const loadNotifications = () => {
    setNotificationsLoading(true);
    // Dummy data for UI testing - will be replaced with API call later
    const dummyNotifications = [
      {
        id: 1,
        message: "Kasus baru dari SKDR",
        createdDate: new Date().toISOString(),
      },
      {
        id: 2,
        message: "Kasus baru dari Kemenkes",
        createdDate: new Date(Date.now() - 3600000).toISOString(),
      },
      {
        id: 3,
        message: "Kasus baru dari SKDR",
        createdDate: new Date(Date.now() - 7200000).toISOString(),
      },
    ];
    setNotifications(dummyNotifications);
    setNotificationsLoading(false);
  };

  const loadData = (res) => {
    // If the response object is not null and has displayItems array with length greater than 0 then set it as data.
    if (res && res.displayItems && res.displayItems.length > 0) {
      setData(res.displayItems);
    } else {
      setData([]);
    }

    // Sets next and previous page numbers based on the total pages and current page number.
    if (res && res.paging) {
      const { totalPages, currentPage } = res.paging;
      if (totalPages > 1) {
        setPagination(true);
        setCurrentApiPage(currentPage);
        setTotalApiPages(totalPages);
        if (parseInt(currentPage) < parseInt(totalPages)) {
          setNextPage(parseInt(currentPage) + 1);
        } else {
          setNextPage(null);
        }

        if (parseInt(currentPage) > 1) {
          setPreviousPage(parseInt(currentPage) - 1);
        } else {
          setPreviousPage(null);
        }
      }
    }

    setLoading(false);
  };

  const loadTimeMetrics = (data) => {
    setTimeMetrics(data);
    setLoading(false);
  };

  const formatNotificationDate = (value: string) => {
    if (!value) return "";
    const date = new Date(value);
    const dateLabel = intl.formatDate(date, {
      weekday: "short",
      day: "2-digit",
      month: "short",
      year: "2-digit",
    });
    const timeLabel = intl.formatTime(date, {
      hour: "2-digit",
      minute: "2-digit",
    });
    return `${dateLabel} ${timeLabel}`;
  };

  const tileList: Array<Tile> = [
    {
      title: <FormattedMessage id="dashboard.in.progress.label" />,
      subTitle: <FormattedMessage id="dashboard.in.progress.subtitle.label" />,
      type: "ORDERS_IN_PROGRESS",
      value: counts.ordersInProgress,
    },
    {
      title: <FormattedMessage id="dashboard.validation.ready.label" />,
      subTitle: (
        <FormattedMessage id="dashboard.validation.ready.subtitle.label" />
      ),
      type: "ORDERS_READY_FOR_VALIDATION",
      value: counts.ordersReadyForValidation,
    },
    {
      title: <FormattedMessage id="dashboard.complete.orders.label" />,
      subTitle: <FormattedMessage id="dashboard.orders.subtitle.label" />,
      type: "ORDERS_COMPLETED_TODAY",
      value: counts.ordersCompletedToday,
    },
    {
      title: <FormattedMessage id="dashboard.partially.completed.label" />,
      subTitle: (
        <FormattedMessage id="dashboard.partially.completed..subtitle.label" />
      ),
      type: "ORDERS_PATIALLY_COMPLETED_TODAY",
      value: counts.patiallyCompletedToday,
    },
    {
      title: <FormattedMessage id="dashboard.user.orders.label" />,
      subTitle: <FormattedMessage id="dashboard.user.orders.subtitle.label" />,
      type: "ORDERS_ENTERED_BY_USER_TODAY",
      value: counts.orderEnterdByUserToday,
    },
    {
      title: <FormattedMessage id="dashboard.rejected.orders" />,
      subTitle: <FormattedMessage id="dashboard.rejected.orders.subtitle" />,
      type: "ORDERS_REJECTED_TODAY",
      value: counts.ordersRejectedToday,
    },
    {
      title: <FormattedMessage id="dashboard.unprints.results.label" />,
      subTitle: (
        <FormattedMessage id="dashboard.unprints.results.subtitle.label" />
      ),
      type: "UN_PRINTED_RESULTS",
      value: counts.unPritendResults,
    },
    {
      title: <FormattedMessage id="sidenav.label.incomingorder" />,
      subTitle: <FormattedMessage id="label.electronic.orders" />,
      type: "INCOMING_ORDERS",
      value: counts.incomigOrders,
    },
    {
      title: <FormattedMessage id="dashboard.avg.turn.around.label" />,
      subTitle: (
        <FormattedMessage id="dashboard.avg.turn.around.subtitle.label" />
      ),
      type: "AVERAGE_TURN_AROUND_TIME",
      value: counts.averageTurnAroudTime,
    },
    {
      title: <FormattedMessage id="dashboard.turn.around.label" />,
      subTitle: <FormattedMessage id="dashboard.turn.around.subtitle.label" />,
      type: "DELAYED_TURN_AROUND",
      value: counts.delayedTurnAround,
    },
  ];

  const averageTimeTileList: Array<Tile> = [
    {
      title: "Reception To Validation Average Time",
      subTitle: "Reception To Validation Average Time",
      type: "AVERAGE_TURN_AROUND_TIME",
      value: timeMetrics.receptionToValidation,
    },
    {
      title: "Reception To Result Average Time",
      subTitle: "Reception To Result Average Time",
      type: "AVERAGE_TURN_AROUND_TIME",
      value: timeMetrics.receptionToResult,
    },
    {
      title: "Result To Validation Average Time",
      subTitle: "Result To Validation Average Time",
      type: "AVERAGE_TURN_AROUND_TIME",
      value: timeMetrics.resultToValidation,
    },
  ];

  const tilesWithTabs = [
    "ORDERS_IN_PROGRESS",
    "ORDERS_READY_FOR_VALIDATION",
    "ORDERS_COMPLETED_TODAY",
    "ORDERS_REJECTED_TODAY",
    "UN_PRINTED_RESULTS",
    "DELAYED_TURN_AROUND",
    "ORDERS_FOR_USER",
    "ORDERS_PATIALLY_COMPLETED_TODAY",
  ];

  const handleMinimizeClick = () => {
    console.log("Icon clicked!");
    if (selectedTile.type == "ORDERS_FOR_USER") {
      const tile: Tile = {
        title: <FormattedMessage id="dashboard.user.orders.label" />,
        subTitle: (
          <FormattedMessage id="dashboard.user.orders.subtitle.label" />
        ),
        type: "ORDERS_ENTERED_BY_USER_TODAY",
        value: counts.orderEnterdByUserToday,
      };
      setSelectedTile(tile);
    } else {
      setSelectedTile(null);
      hasRole(userSessionDetails, "Global Administrator")
        ? setSelectedTestSection("all")
        : setSelectedTestSection(testSections[0]?.id);
    }
  };

  const handleMaximizeClick = (tile) => {
    if (
      testSections?.length > 0 ||
      hasRole(userSessionDetails, "Global Administrator")
    ) {
      setSelectedTile(tile);
    } else {
      setNotificationVisible(true);
      addNotification({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "accessDenied.title" }),
        message: intl.formatMessage({ id: "accessDenied.message" }),
      });
    }
  };

  const viewUserOrders = (row) => {
    console.log("Icon clicked!");
    const firstName = row.cells.find(
      (e) => e.info.header === "userFirstName",
    ).value;
    const lastName = row.cells.find(
      (e) => e.info.header === "userLastName",
    ).value;
    const value = row.cells.find(
      (e) => e.info.header === "countOfOrdersEntered",
    ).value;

    const tile: Tile = {
      title: <FormattedMessage id="dashboard.user.orders.today.label" />,
      subTitle: firstName + " " + lastName,
      type: "ORDERS_FOR_USER",
      value: value,
      id: row.id,
    };
    setSelectedTile(tile);
  };

  const handlePageChange = (pageInfo) => {
    if (page != pageInfo.page) {
      setPage(pageInfo.page);
    }

    if (pageSize != pageInfo.pageSize) {
      setPageSize(pageInfo.pageSize);
    }
  };
  const renderCell = (cell, row) => {
    if (cell.info.header === "labNumber" && cell.value) {
      return (
        <TableCell key={cell.id}>
          <>
            <div style={{ display: "flex", alignItems: "center" }}>
              <Button
                onClick={async () => {
                  if ("clipboard" in navigator) {
                    return await navigator.clipboard.writeText(cell.value);
                  } else {
                    return document.execCommand("copy", true, cell.value);
                  }
                }}
                kind="ghost"
                iconDescription={intl.formatMessage({
                  id: "instructions.copy.labnum",
                })}
                hasIconOnly
                renderIcon={Copy}
              />
              {selectedTile.type == "ORDERS_IN_PROGRESS" ||
              selectedTile.type == "ORDERS_READY_FOR_VALIDATION" ? (
                <Link
                  style={{ color: "blue" }}
                  href={
                    selectedTile.type == "ORDERS_IN_PROGRESS"
                      ? "/result?type=order&doRange=false&accessionNumber=" +
                        cell.value
                      : "validation?type=order&accessionNumber=" + cell.value
                  }
                >
                  <u>{convertAlphaNumLabNumForDisplay(cell.value)}</u>
                </Link>
              ) : (
                <> {convertAlphaNumLabNumForDisplay(cell.value)}</>
              )}
            </div>
          </>
        </TableCell>
      );
    } else if (cell.info.header === "countOfOrdersEntered" && cell.value) {
      return (
        <TableCell key={cell.id}>
          <Link style={{ color: "blue" }}>{cell.value} </Link>
        </TableCell>
      );
    } else {
      return <TableCell key={cell.id}>{cell.value}</TableCell>;
    }
  };

  const orderHeaders = [
    {
      key: "priority",
      header: <FormattedMessage id="eorder.priority" />,
    },
    {
      key: "orderDate",
      header: <FormattedMessage id="sample.label.orderdate" />,
    },
    {
      key: "patientId",
      header: <FormattedMessage id="patient.id" />,
    },
    {
      key: "labNumber",
      header: <FormattedMessage id="eorder.labNumber" />,
    },
    {
      key: "testName",
      header: <FormattedMessage id="eorder.test.name" />,
    },
  ];

  const userHeaders = [
    {
      key: "userFirstName",
      header: "First Name",
    },
    {
      key: "userLastName",
      header: "Last Name",
    },
    {
      key: "countOfOrdersEntered",
      header: "Orders Entered",
    },
  ];

  const headers = [
    {
      key: "disease",
      header: intl.formatMessage({
        id: "surveillance.table.disease",
        defaultMessage: "Disease",
      }),
    },
    {
      key: "region",
      header: intl.formatMessage({
        id: "surveillance.table.region",
        defaultMessage: "Region",
      }),
    },
    {
      key: "cases",
      header: intl.formatMessage({
        id: "label.number.case",
        defaultMessage: "Number of Cases",
      }),
    },
  ];

  const indonesiaMapData = useMemo(
    () =>
      regionalTotals
        .map((item) => {
          const code = REGION_CODE_MAP[item.region?.toLowerCase() || ""];

          if (!code) {
            return null;
          }

          return {
            code,
            name: item.region,
            value: item.total,
          };
        })
        .filter(Boolean),
    [regionalTotals],
  );

  const surveillanceRows = useMemo(
    () =>
      regionalTotals.map((item, index) => ({
        id: `${item.icdCode}-${item.region || "unknown"}-${index}`,
        disease: item.name,
        region:
          item.region ||
          intl.formatMessage({
            id: "surveillance.table.region.unassigned",
            defaultMessage: "Unassigned",
          }),
        cases: `${item.total}`,
      })),
    [intl, regionalTotals],
  );

  function formatFilterDate(dateValue: Date | null) {
    if (!(dateValue instanceof Date) || Number.isNaN(dateValue.getTime())) {
      return null;
    }

    return dateValue.toISOString().split("T")[0];
  }

  function loadDiseaseAggregates() {
    const query = new URLSearchParams();
    const startDate = formatFilterDate(dateRange.start);
    const endDate = formatFilterDate(dateRange.end);

    if (diseaseFilter) {
      query.append("icdCode", diseaseFilter);
    }
    if (startDate) {
      query.append("startDate", startDate);
    }
    if (endDate) {
      query.append("endDate", endDate);
    }

    const endpoint = `/rest/surveillance/disease-aggregates${query.toString() ? `?${query.toString()}` : ""}`;

    setSurveillanceDataLoading(true);
    getFromOpenElisServer(endpoint, (response) => {
      if (!componentMounted.current) {
        return;
      }

      setSurveillanceDataLoading(false);

      if (!response) {
        setNationalTotals([]);
        setRegionalTotals([]);
        return;
      }

      const national = (response.national || []).map((item) => ({
        icdCode: item.icdCode,
        name: item.diseaseName,
        total: item.totalCases,
      }));
      const regional = (response.regional || []).map((item) => ({
        icdCode: item.icdCode,
        name: item.diseaseName,
        total: item.totalCases,
        region: item.region,
      }));
      setNationalTotals(national);
      setRegionalTotals(regional);
    });
  }

  return (
    <>
      {loading && <Loading description="Loading Dasboard..." />}
      {notificationVisible === true ? <AlertDialog /> : ""}
      {selectedTile == null ? (
        <>
          <div className="surveillance-stage">
            <div className="surveillance-grid">
              <div className="surveillance-tiles-column">
                <div className="surveillance-container">
                  {tileList.map((tile, index) => (
                    <ClickableTile
                      key={index}
                      className="surveillance-tile"
                      onClick={() => handleMaximizeClick(tile)}
                    >
                      <h3 className="tile-title">{tile.title}</h3>
                      <p className="tile-subtitle">{tile.subTitle}</p>
                      <p className="tile-value">{tile.value}</p>

                      <div className="tile-icon">
                        <div
                          onClick={() => handleMaximizeClick(tile)}
                          className="icon-wrapper"
                        >
                          <Maximize
                            id="maximizeIcon"
                            size={20}
                            className="clickable-icon"
                          />
                        </div>
                      </div>
                    </ClickableTile>
                  ))}
                </div>
              </div>
              <div className="surveillance-notifications">
                <div className="notifications-header">
                  <p className="notifications-title">
                    <FormattedMessage
                      id="dashboard.notifications.title"
                      defaultMessage="Notifications"
                    />
                  </p>
                </div>
                {notificationsLoading ? (
                  <div className="notifications-empty">
                    <Loading description="" />
                  </div>
                ) : !notifications.length ? (
                  <div className="notifications-empty">
                    <p>
                      <FormattedMessage
                        id="dashboard.notifications.empty"
                        defaultMessage="No notifications"
                      />
                    </p>
                  </div>
                ) : (
                  notifications.slice(0, 4).map((notification) => (
                    <article
                      className="notification-card"
                      key={notification.id}
                    >
                      <p className="notification-message">
                        {notification.message}
                      </p>
                      <div className="notification-footer">
                        <span className="notification-date">
                          {formatNotificationDate(notification.createdDate)}
                        </span>
                        <Link className="notifications-link" href="#">
                          <FormattedMessage
                            id="dashboard.notifications.viewDetail"
                            defaultMessage="View details"
                          />
                        </Link>
                      </div>
                    </article>
                  ))
                )}
              </div>
            </div>
          </div>
          <Map
            data={indonesiaMapData.length ? indonesiaMapData : undefined}
            disease={selectedDiseaseOption?.label || disease}
            threshold={threshold}
            onDownload={() => {
              // TODO: hook this up to a real export endpoint when available.
            }}
          />

          <div className="surveillance-filters">
            <div className="surveillance-filter-column">
              <label className="surveillance-filter-label">
                {intl.formatMessage({
                  id: "surveillance.filter.disease",
                  defaultMessage: "Disease",
                })}
              </label>
              <Dropdown
                id="disease-filter"
                titleText=""
                light
                placeholder={intl.formatMessage({
                  id: "surveillance.filter.placeholder",
                  defaultMessage: "Select",
                })}
                items={diseaseOptions}
                itemToString={(item) => item?.label || ""}
                selectedItem={diseaseOptions.find(
                  (item) => item.id === diseaseFilter,
                )}
                onChange={({ selectedItem }) =>
                  setDiseaseFilter(selectedItem?.id || null)
                }
              />
            </div>
            <div className="surveillance-filter-column">
              <label className="surveillance-filter-label">
                {intl.formatMessage({
                  id: "surveillance.filter.from",
                  defaultMessage: "From",
                })}
              </label>
              <DatePicker
                datePickerType="single"
                onChange={(dates) =>
                  setDateRange({
                    start: dates[0],
                    end: dateRange.end,
                  })
                }
              >
                <DatePickerInput
                  id="date-start"
                  placeholder="YYYY-MM-DD"
                  labelText=""
                  size="md"
                  dateFormat="Y-m-d"
                  value={
                    dateRange.start
                      ? dateRange.start.toISOString().split("T")[0]
                      : ""
                  }
                />
              </DatePicker>
            </div>
            <div className="surveillance-filter-column">
              <label className="surveillance-filter-label">
                {intl.formatMessage({
                  id: "surveillance.filter.to",
                  defaultMessage: "To",
                })}
              </label>
              <DatePicker
                datePickerType="single"
                onChange={(dates) =>
                  setDateRange({
                    start: dateRange.start,
                    end: dates[0],
                  })
                }
              >
                <DatePickerInput
                  id="date-end"
                  placeholder="YYYY-MM-DD"
                  labelText=""
                  size="md"
                  dateFormat="Y-m-d"
                  value={
                    dateRange.end
                      ? dateRange.end.toISOString().split("T")[0]
                      : ""
                  }
                />
              </DatePicker>
            </div>
            <div className="surveillance-filter-actions">
              <button
                className="surveillance-action primary"
                type="button"
                onClick={loadDiseaseAggregates}
              >
                {intl.formatMessage({
                  id: "surveillance.button.search",
                  defaultMessage: "Search",
                })}
              </button>
              <button className="surveillance-action secondary" type="button">
                {intl.formatMessage({
                  id: "surveillance.button.download",
                  defaultMessage: "Download",
                })}
                <span className="surveillance-dropdown-icon">▾</span>
              </button>
            </div>
          </div>

          <div className="surveillance-map-trend-card">
            <div className="surveillance-map-card">
              <div className="surveillance-map-trend-chart">
                <HighchartsReact
                  highcharts={Highcharts}
                  options={trendChartOptions}
                />
                {surveillanceDataLoading && (
                  <Loading withOverlay={false} description="" small />
                )}
              </div>
            </div>
          </div>

          <div className="surveillance-filters">
            <div className="surveillance-filter-column">
              <label className="surveillance-filter-label">
                {intl.formatMessage({
                  id: "surveillance.filter.disease",
                  defaultMessage: "Disease",
                })}
              </label>
              <Dropdown
                id="disease-filter"
                titleText=""
                light
                placeholder={intl.formatMessage({
                  id: "surveillance.filter.placeholder",
                  defaultMessage: "Select",
                })}
                items={diseaseOptions}
                itemToString={(item) => item?.label || ""}
                selectedItem={diseaseOptions.find(
                  (item) => item.id === diseaseFilter,
                )}
                onChange={({ selectedItem }) =>
                  setDiseaseFilter(selectedItem?.id || null)
                }
              />
            </div>
            <div className="surveillance-filter-column">
              <label className="surveillance-filter-label">
                {intl.formatMessage({
                  id: "surveillance.filter.status",
                  defaultMessage: "Status",
                })}
              </label>
              <Dropdown
                id="status-filter"
                titleText=""
                light
                placeholder={intl.formatMessage({
                  id: "surveillance.filter.placeholder",
                  defaultMessage: "Select",
                })}
                items={STATUS_OPTIONS}
                itemToString={(item) => item?.label || ""}
                selectedItem={STATUS_OPTIONS.find((s) => s.id === statusFilter)}
                onChange={({ selectedItem }) =>
                  setStatusFilter(selectedItem?.id || null)
                }
              />
            </div>
            <div className="surveillance-filter-column">
              <label className="surveillance-filter-label">
                {intl.formatMessage({
                  id: "surveillance.filter.from",
                  defaultMessage: "From",
                })}
              </label>
              <DatePicker
                datePickerType="single"
                onChange={(dates) =>
                  setDateRange({
                    start: dates[0],
                    end: dateRange.end,
                  })
                }
              >
                <DatePickerInput
                  id="date-start"
                  placeholder="YYYY-MM-DD"
                  labelText=""
                  size="md"
                  dateFormat="Y-m-d"
                  value={
                    dateRange.start
                      ? dateRange.start.toISOString().split("T")[0]
                      : ""
                  }
                />
              </DatePicker>
            </div>
            <div className="surveillance-filter-column">
              <label className="surveillance-filter-label">
                {intl.formatMessage({
                  id: "surveillance.filter.to",
                  defaultMessage: "To",
                })}
              </label>
              <DatePicker
                datePickerType="single"
                onChange={(dates) =>
                  setDateRange({
                    start: dateRange.start,
                    end: dates[0],
                  })
                }
              >
                <DatePickerInput
                  id="date-end"
                  placeholder="YYYY-MM-DD"
                  labelText=""
                  size="md"
                  dateFormat="Y-m-d"
                  value={
                    dateRange.end
                      ? dateRange.end.toISOString().split("T")[0]
                      : ""
                  }
                />
              </DatePicker>
            </div>
            <div className="surveillance-filter-actions">
              <button
                className="surveillance-action primary"
                type="button"
                onClick={loadDiseaseAggregates}
              >
                {intl.formatMessage({
                  id: "surveillance.button.search",
                  defaultMessage: "Search",
                })}
              </button>
              <button className="surveillance-action secondary" type="button">
                {intl.formatMessage({
                  id: "surveillance.button.download",
                  defaultMessage: "Download",
                })}
                <span className="surveillance-dropdown-icon">▾</span>
              </button>
            </div>
          </div>

          <div className="surveillance-table-container">
            <DataTable rows={surveillanceRows} headers={headers} isSortable>
              {({
                rows,
                headers,
                getTableProps,
                getHeaderProps,
                getRowProps,
              }) => (
                <TableContainer>
                  <Table {...getTableProps()}>
                    <TableHead>
                      <TableRow>
                        {headers.map((header) => (
                          <TableHeader
                            key={header.key}
                            {...getHeaderProps({ header })}
                          >
                            {header.header}
                          </TableHeader>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.length === 0 ? (
                        <TableRow>
                          <TableCell
                            colSpan={headers.length}
                            className="empty-table"
                          >
                            {/* {isLoading ? (
                            <FormattedMessage
                              id="loading"
                              defaultMessage="Loading..." />
                          ) : ( */}
                            <FormattedMessage
                              id="dashboard.disease.empty"
                              defaultMessage="No disease found."
                            />
                            {/* )} */}
                          </TableCell>
                        </TableRow>
                      ) : (
                        rows.map((row) => (
                          <TableRow
                            key={row.id}
                            {...getRowProps({ row })}
                            // className={orders.find(
                            //   (o) => o.id === row.id || o.labNumber === row.id
                            // )?.returnedFromQA
                            //   ? "returned-from-qa"
                            //   : ""}
                          >
                            {row.cells.map((cell) => (
                              <TableCell key={cell.id}>{cell.value}</TableCell>
                            ))}
                          </TableRow>
                        ))
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </DataTable>
            <Pagination
              totalItems={data.length}
              pageSize={pageSize}
              pageSizes={PAGE_SIZES}
              page={page}
              onChange={({ page: newPage, pageSize: newPageSize }) => {
                setPage(newPage);
                setPageSize(newPageSize);
              }}
            />
          </div>
        </>
      ) : (
        <div className="surveillance-view">
          <Tile className="surveillance-tile">
            <Grid>
              <Column lg={16} md={8} sm={4}>
                <h3 className="tile-title-view">{selectedTile.title}</h3>
                <p className="tile-subtitle-view">{selectedTile.subTitle}</p>
                <p className="tile-value-view">{selectedTile.value}</p>
                {
                  <div className="tile-icon">
                    <div onClick={handleMinimizeClick} className="icon-wrapper">
                      <Minimize
                        id="minimizeIcon"
                        size={20}
                        className="clickable-icon"
                      />
                    </div>
                  </div>
                }
              </Column>
            </Grid>
            <div className="gridBoundary">
              {selectedTile.type == "AVERAGE_TURN_AROUND_TIME" ? (
                <>
                  <div className="surveillance-container">
                    {averageTimeTileList.map((tile, index) => (
                      <Tile key={index} className="surveillance-tile">
                        <h3 className="tile-title">{tile.title}</h3>
                        <p className="tile-subtitle">{tile.subTitle}</p>
                        <p className="tile-value">{tile.value}</p>
                      </Tile>
                    ))}
                  </div>
                </>
              ) : (
                <Grid>
                  <Column lg={16} md={8} sm={4}>
                    {pagination && (
                      <Grid>
                        <Column lg={14} />
                        <Column
                          lg={2}
                          style={{
                            display: "flex",
                            flexDirection: "column",
                            alignItems: "center",
                            gap: "10px",
                            width: "110%",
                          }}
                        >
                          <Link>
                            {currentApiPage} / {totalApiPages}
                          </Link>
                          <div style={{ display: "flex", gap: "10px" }}>
                            <Button
                              hasIconOnly
                              id="loadpreviousresults"
                              onClick={loadPreviousResultsPage}
                              disabled={previousPage != null ? false : true}
                              renderIcon={ArrowLeft}
                              iconDescription="previous"
                            ></Button>
                            <Button
                              hasIconOnly
                              id="loadnextresults"
                              onClick={loadNextResultsPage}
                              disabled={nextPage != null ? false : true}
                              renderIcon={ArrowRight}
                              iconDescription="next"
                            ></Button>
                          </div>
                        </Column>
                      </Grid>
                    )}
                    {tilesWithTabs.includes(selectedTile.type) && (
                      <Grid>
                        <Column lg={16} md={8} sm={4}>
                          <Tabs>
                            {hasRole(
                              userSessionDetails,
                              "Global Administrator",
                            ) ? (
                              <TabList
                                style={{ width: "100%" }}
                                aria-label="List of tabs"
                                contained
                              >
                                <Tab
                                  onClick={() => setSelectedTestSection("all")}
                                >
                                  <FormattedMessage id="all.label" />
                                </Tab>

                                {testSections?.map((item, id) => {
                                  return (
                                    <Tab
                                      key={id}
                                      onClick={() =>
                                        setSelectedTestSection(item.id)
                                      }
                                    >
                                      {item.value}
                                    </Tab>
                                  );
                                })}
                              </TabList>
                            ) : (
                              <TabList
                                style={{ width: "100%" }}
                                aria-label="List of tabs"
                                contained
                              >
                                {testSections?.map((item, id) => {
                                  return (
                                    <Tab
                                      key={id}
                                      onClick={() =>
                                        setSelectedTestSection(item.id)
                                      }
                                    >
                                      {item.value}
                                    </Tab>
                                  );
                                })}
                              </TabList>
                            )}
                          </Tabs>
                        </Column>
                      </Grid>
                    )}
                    <DataTable
                      rows={data
                        .filter((item) =>
                          tilesWithTabs.includes(selectedTile.type) &&
                          selectedTestSection != "all"
                            ? item.testSection === selectedTestSection
                            : true,
                        )
                        .slice((page - 1) * pageSize, page * pageSize)}
                      headers={
                        selectedTile.type != "ORDERS_ENTERED_BY_USER_TODAY"
                          ? orderHeaders
                          : userHeaders
                      }
                      isSortable
                    >
                      {({ rows, headers, getHeaderProps, getTableProps }) => (
                        <TableContainer title="" description="">
                          <Table {...getTableProps()}>
                            <TableHead>
                              <TableRow>
                                {headers.map((header) => (
                                  <TableHeader
                                    key={header.key}
                                    {...getHeaderProps({ header })}
                                  >
                                    {header.header}
                                  </TableHeader>
                                ))}
                              </TableRow>
                            </TableHead>
                            <TableBody>
                              <>
                                {rows.map((row) => (
                                  <TableRow
                                    key={row.id}
                                    onClick={() => {
                                      selectedTile.type ==
                                      "ORDERS_ENTERED_BY_USER_TODAY"
                                        ? viewUserOrders(row)
                                        : {};
                                    }}
                                  >
                                    {row.cells.map((cell) =>
                                      renderCell(cell, row),
                                    )}
                                  </TableRow>
                                ))}
                              </>
                            </TableBody>
                          </Table>
                        </TableContainer>
                      )}
                    </DataTable>
                    <Pagination
                      onChange={handlePageChange}
                      page={page}
                      pageSize={pageSize}
                      pageSizes={[10, 20, 30, 50, 100]}
                      totalItems={
                        data.filter((item) =>
                          tilesWithTabs.includes(selectedTile.type) &&
                          selectedTestSection != "all"
                            ? item.testSection === selectedTestSection
                            : true,
                        ).length
                      }
                      forwardText={intl.formatMessage({
                        id: "pagination.forward",
                      })}
                      backwardText={intl.formatMessage({
                        id: "pagination.backward",
                      })}
                      itemRangeText={(min, max, total) =>
                        intl.formatMessage(
                          { id: "pagination.item-range" },
                          { min: min, max: max, total: total },
                        )
                      }
                      itemsPerPageText={intl.formatMessage({
                        id: "pagination.items-per-page",
                      })}
                      itemText={(min, max) =>
                        intl.formatMessage(
                          { id: "pagination.item" },
                          { min: min, max: max },
                        )
                      }
                      pageNumberText={intl.formatMessage({
                        id: "pagination.page-number",
                      })}
                      pageRangeText={(_current, total) =>
                        intl.formatMessage(
                          { id: "pagination.page-range" },
                          { total: total },
                        )
                      }
                      pageText={(page, pagesUnknown) =>
                        intl.formatMessage(
                          { id: "pagination.page" },
                          { page: pagesUnknown ? "" : page },
                        )
                      }
                    />
                  </Column>
                </Grid>
              )}
            </div>
          </Tile>
        </div>
      )}
    </>
  );
};
export default Surveillance;
