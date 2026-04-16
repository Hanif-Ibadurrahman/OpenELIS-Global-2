import { useState, useEffect, useContext } from "react";
import {
  Accordion,
  AccordionItem,
  Button,
  Modal,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  TextArea,
  Select,
  SelectItem,
  ComboBox,
  FilterableMultiSelect,
  Loading,
  Pagination,
  Dropdown,
  DatePicker,
  DatePickerInput,
  Grid,
  Column,
  Section,
  Heading,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { Edit } from "@carbon/icons-react";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  putToOpenElisServerFullResponse,
  deleteFromOpenElisServerFullResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import "./SurveillanceConfiguration.css";

const STATUS_OPTIONS = [
  { id: "all", label: "All Status" },
  { id: "active", label: "Active" },
  { id: "inactive", label: "Inactive" },
];

const RESULT_TYPE_OPTIONS = [
  { id: "D", label: "Discrete (text match)" },
  { id: "N", label: "Numeric" },
];

const NUMERIC_OPERATOR_OPTIONS = [
  { id: "EQ", label: "= (Equal)" },
  { id: "LT", label: "< (Less than)" },
  { id: "GT", label: "> (Greater than)" },
  { id: "LTE", label: "<= (Less or equal)" },
  { id: "GTE", label: ">= (Greater or equal)" },
];

const TIME_RANGE_OPTIONS = [
  { id: "", label: "Langsung" },
  { id: "daily", label: "Harian" },
  { id: "weekly", label: "Mingguan" },
  { id: "monthly", label: "Bulanan" },
];

const THRESHOLD_TYPE_OPTIONS = [
  { id: "", label: "-" },
  { id: "single_case", label: "Single Case" },
  { id: "count", label: "Count" },
  { id: "rate", label: "Rate" },
  { id: "cluster", label: "Cluster" },
  { id: "trend", label: "Trend" },
];

const NOTIFICATION_TYPE_OPTIONS = [
  { id: "all", label: "Email + SMS" },
  { id: "email", label: "Email" },
  { id: "sms", label: "SMS" },
];

const EMPTY_RULE = {
  ruleGroup: 1,
  operator: "",
  testName: "",
  resultType: "D",
  resultValue: "",
  numericOperator: "",
  numericValue: "",
};

const initialCreateForm = {
  icdCode: "",
  version: 1,
  description: "",
  effectiveDate: "",
  isActive: true,
  timeRange: "",
  thresholdType: "",
  thresholdValue: "",
  rules: [{ ...EMPTY_RULE }],
  recipients: [],
};

const SurveillanceConfiguration = () => {
  const intl = useIntl();
  const { addNotification } = useContext(NotificationContext);

  const [localizations, setLocalizations] = useState([]);
  const [searchText, setSearchText] = useState("");
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [editValues, setEditValues] = useState({});
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [createForm, setCreateForm] = useState(initialCreateForm);
  const [icdCodeOptions, setIcdCodeOptions] = useState([]);
  const [filteredIcdCodeOptions, setFilteredIcdCodeOptions] = useState([]);
  const [diseaseFilter, setDiseaseFilter] = useState(null);
  const [statusFilter, setStatusFilter] = useState(null);
  const [definitionRules, setDefinitionRules] = useState([]);
  const [rulesLoading, setRulesLoading] = useState(false);
  const [tableLoading, setTableLoading] = useState(false);
  const [testOptions, setTestOptions] = useState([]);
  const [personOptions, setPersonOptions] = useState([]);

  const loadDefinitions = () => {
    setTableLoading(true);
    getFromOpenElisServer("/rest/surveillance/case-definitions", (response) => {
      setTableLoading(false);
      if (Array.isArray(response)) {
        setLocalizations(response);
      }
    });
  };

  useEffect(() => {
    loadDefinitions();
    getFromOpenElisServer("/rest/surveillance/icd-codes", (response) => {
      if (Array.isArray(response)) {
        const opts = response.map((item) => ({
          id: item.code,
          label: `${item.code} - ${item.title}`,
          code: item.code,
        }));
        setIcdCodeOptions(opts);
        setFilteredIcdCodeOptions(opts);
      }
    });
    getFromOpenElisServer("/rest/tests", (response) => {
      if (Array.isArray(response)) {
        const opts = response.map((item) => ({
          id: item.value,
          label: item.value,
        }));
        setTestOptions(opts);
      }
    });
    getFromOpenElisServer("/rest/surveillance/persons", (response) => {
      if (Array.isArray(response)) {
        const opts = response.map((item) => {
          const contact = [item.email, item.cellPhone || item.workPhone]
            .filter(Boolean)
            .join(" / ");
          return {
            id: String(item.id),
            label: contact ? `${item.name} (${contact})` : item.name,
            email: item.email,
            workPhone: item.workPhone,
            cellPhone: item.cellPhone,
          };
        });
        setPersonOptions(opts);
      }
    });
  }, []);

  const handleEdit = (item) => {
    setEditingItem(item);
    setCreateForm({
      icdCode: item.icdCode || "",
      version: item.version || 1,
      description: item.description || "",
      effectiveDate: item.effectiveDate || "",
      isActive: item.isActive !== undefined ? item.isActive : true,
      timeRange: item.timeRange || "",
      thresholdType: item.thresholdType || "",
      thresholdValue:
        item.thresholdValue !== undefined && item.thresholdValue !== null
          ? String(item.thresholdValue)
          : "",
      rules: [{ ...EMPTY_RULE }],
      recipients: [],
    });
    setRulesLoading(true);
    getFromOpenElisServer(
      `/rest/surveillance/case-rules?definitionId=${item.id}`,
      (response) => {
        setRulesLoading(false);
        const rules =
          Array.isArray(response) && response.length > 0
            ? response.map((r) => ({
                id: r.id,
                ruleGroup: r.ruleGroup || 1,
                operator: r.operator || "AND",
                testName: r.testName || "",
                resultType: r.resultType || "D",
                resultValue: r.resultValue || "",
                numericOperator: r.numericOperator || "",
                numericValue: r.numericValue || "",
              }))
            : [{ ...EMPTY_RULE }];
        setDefinitionRules(rules);
        setCreateForm((prev) => ({ ...prev, rules }));
      },
    );
    getFromOpenElisServer(
      `/rest/surveillance/case-definitions/${item.id}/recipients`,
      (response) => {
        if (Array.isArray(response)) {
          const recipients = response.map((r) => ({
            personId: String(r.personId),
            notificationType: r.notificationType || "all",
          }));
          setCreateForm((prev) => ({ ...prev, recipients }));
        }
      },
    );
    setIsCreateModalOpen(true);
  };

  const buildDefinitionPayload = () => ({
    icdCode: createForm.icdCode,
    version: createForm.version,
    description: createForm.description,
    effectiveDate: createForm.effectiveDate || null,
    isActive: createForm.isActive,
    timeRange: createForm.timeRange || null,
    thresholdType: createForm.thresholdType || null,
    thresholdValue:
      createForm.thresholdValue === "" || createForm.thresholdValue === null
        ? null
        : parseFloat(createForm.thresholdValue),
  });

  const saveRecipients = (definitionId) => {
    const payload = (createForm.recipients || []).map((r) => ({
      personId: r.personId,
      notificationType: r.notificationType || "all",
    }));
    return fetch(
      `/rest/surveillance/case-definitions/${definitionId}/recipients`,
      {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(payload),
      },
    );
  };

  const handleSave = () => {
    if (!editingItem) return;
    const payload = buildDefinitionPayload();
    putToOpenElisServerFullResponse(
      `/rest/surveillance/case-definitions/${editingItem.id}`,
      JSON.stringify(payload),
      (response) => {
        if (response.ok) {
          saveRules(editingItem.id, true);
        } else {
          addNotification({
            kind: "error",
            title: intl.formatMessage({
              id: "notification.error",
              defaultMessage: "Error",
            }),
            message: intl.formatMessage({
              id: "surveillance.save.error",
              defaultMessage: "Failed to save definition",
            }),
          });
        }
      },
    );
  };

  const saveRules = (definitionId, isEdit) => {
    const existingRuleIds = createForm.rules
      .filter((r) => r.id)
      .map((r) => r.id);
    Promise.all(
      createForm.rules.map((rule) => {
        const rulePayload = {
          definitionId,
          ruleGroup: rule.ruleGroup,
          operator: rule.operator,
          testName: rule.testName,
          resultType: rule.resultType,
          resultValue: rule.resultValue,
          numericOperator: rule.numericOperator || null,
          numericValue: rule.numericValue
            ? parseFloat(rule.numericValue)
            : null,
        };
        if (rule.id) {
          return fetch(`/rest/surveillance/case-rules/${rule.id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            credentials: "include",
            body: JSON.stringify(rulePayload),
          });
        } else {
          return fetch("/rest/surveillance/case-rules", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "include",
            body: JSON.stringify(rulePayload),
          });
        }
      }),
    )
      .then(() => saveRecipients(definitionId))
      .then(() => {
        addNotification({
          kind: "success",
          title: intl.formatMessage({
            id: "notification.success",
            defaultMessage: "Success",
          }),
          message: intl.formatMessage({
            id: "surveillance.save.success",
            defaultMessage: "Saved successfully",
          }),
        });
        setIsCreateModalOpen(false);
        setEditingItem(null);
        setCreateForm(initialCreateForm);
        setDefinitionRules([]);
        loadDefinitions();
      });
  };

  const handleDelete = (item) => {
    if (
      !window.confirm(
        intl.formatMessage({
          id: "surveillance.delete.confirm",
          defaultMessage: "Delete this definition?",
        }),
      )
    )
      return;
    deleteFromOpenElisServerFullResponse(
      `/rest/surveillance/case-definitions/${item.id}`,
      (response) => {
        if (response.ok) {
          addNotification({
            kind: "success",
            title: intl.formatMessage({
              id: "notification.success",
              defaultMessage: "Success",
            }),
            message: intl.formatMessage({
              id: "surveillance.delete.success",
              defaultMessage: "Deleted successfully",
            }),
          });
          loadDefinitions();
        }
      },
    );
  };

  const handleCreateChange = (field, value) => {
    setCreateForm((prev) => ({ ...prev, [field]: value }));
  };

  const handleIcdCodeQuery = (query) => {
    const normalized = query ? query.toLowerCase() : "";
    setFilteredIcdCodeOptions(
      icdCodeOptions.filter((option) =>
        option.label.toLowerCase().includes(normalized),
      ),
    );
  };

  const handleRuleChange = (index, field, value) => {
    setCreateForm((prev) => {
      const rules = [...prev.rules];
      rules[index] = { ...rules[index], [field]: value };
      return { ...prev, rules };
    });
  };

  const addRule = () => {
    setCreateForm((prev) => ({
      ...prev,
      rules: [...prev.rules, { ...EMPTY_RULE }],
    }));
  };

  const removeRule = (index) => {
    setCreateForm((prev) => ({
      ...prev,
      rules: prev.rules.filter((_, i) => i !== index),
    }));
  };

  const isFirstInGroup = (idx, ruleGroup) => {
    const rules = createForm.rules || [];
    const sameGroupRules = rules.filter(
      (r, i) => r.ruleGroup === ruleGroup && i < idx,
    );
    return sameGroupRules.length === 0;
  };

  const handleCreateSubmit = () => {
    const payload = {
      ...buildDefinitionPayload(),
      version: createForm.version || 1,
    };
    postToOpenElisServerFullResponse(
      "/rest/surveillance/case-definitions",
      JSON.stringify(payload),
      (response) => {
        if (response.ok) {
          response.json().then((created) => {
            saveRules(created.id, false);
          });
        } else {
          addNotification({
            kind: "error",
            title: intl.formatMessage({
              id: "notification.error",
              defaultMessage: "Error",
            }),
            message: intl.formatMessage({
              id: "surveillance.create.error",
              defaultMessage: "Failed to create definition",
            }),
          });
        }
      },
    );
  };

  const openCreateModal = () => {
    setEditingItem(null);
    setCreateForm(initialCreateForm);
    setDefinitionRules([]);
    setIsCreateModalOpen(true);
  };

  const closeCreateModal = () => {
    setIsCreateModalOpen(false);
    setEditingItem(null);
    setCreateForm(initialCreateForm);
    setDefinitionRules([]);
  };

  const filteredLocalizations = localizations.filter((item) => {
    const matchesStatus =
      !statusFilter || statusFilter === "all"
        ? true
        : statusFilter === "active"
          ? item.isActive
          : !item.isActive;
    const matchesIcd =
      !diseaseFilter || diseaseFilter === "all"
        ? true
        : item.icdCode === diseaseFilter;
    if (!matchesStatus || !matchesIcd) return false;
    if (!searchText) return true;
    const search = searchText.toLowerCase();
    return (
      (item.icdCode && item.icdCode.toLowerCase().includes(search)) ||
      (item.diseaseName && item.diseaseName.toLowerCase().includes(search)) ||
      (item.description && item.description.toLowerCase().includes(search))
    );
  });

  const paginatedLocalizations = filteredLocalizations.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize,
  );

  const headers = [
    {
      key: "id",
      header: intl.formatMessage({ id: "label.id", defaultMessage: "ID" }),
    },
    {
      key: "diseaseName",
      header: intl.formatMessage({
        id: "label.disease.name",
        defaultMessage: "Disease Name",
      }),
    },
    {
      key: "icdCode",
      header: intl.formatMessage({
        id: "label.icd.code",
        defaultMessage: "ICD Code",
      }),
    },
    {
      key: "version",
      header: intl.formatMessage({
        id: "label.version",
        defaultMessage: "Version",
      }),
    },
    {
      key: "description",
      header: intl.formatMessage({
        id: "label.description",
        defaultMessage: "Description",
      }),
    },
    {
      key: "isActive",
      header: intl.formatMessage({
        id: "label.status",
        defaultMessage: "Status",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "label.actions",
        defaultMessage: "Actions",
      }),
    },
  ];

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
    {
      label: "breadcrumb.surveillance",
      link: "/MasterListsPage/SurveillanceConfiguration",
    },
  ];

  return (
    <>
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage
                  id="surveillance.title"
                  defaultMessage="Surveillance Configuration"
                />
              </Heading>
            </Section>
          </Column>
        </Grid>
        <Grid className="surveillance-filters">
          <Column sm={4} md={3} lg={4}>
            <Dropdown
              id="status-filter"
              titleText={intl.formatMessage({
                id: "surveillance.filter.status",
                defaultMessage: "Status",
              })}
              placeholder={intl.formatMessage({
                id: "surveillance.filter.placeholder",
                defaultMessage: "All Status",
              })}
              items={STATUS_OPTIONS}
              itemToString={(item) => item?.label || ""}
              selectedItem={
                STATUS_OPTIONS.find((s) => s.id === statusFilter) || null
              }
              onChange={({ selectedItem }) =>
                setStatusFilter(selectedItem?.id || null)
              }
            />
          </Column>
          <Column sm={4} md={3} lg={4}>
            <ComboBox
              id="icd-filter"
              titleText={intl.formatMessage({
                id: "label.icd.code",
                defaultMessage: "ICD Code",
              })}
              placeholder={intl.formatMessage({
                id: "surveillance.filter.placeholder",
                defaultMessage: "All diseases",
              })}
              items={[
                { id: "all", label: "All Diseases", code: "all" },
                ...icdCodeOptions,
              ]}
              itemToString={(item) => item?.label || ""}
              onInputChange={handleIcdCodeQuery}
              onChange={({ selectedItem }) =>
                setDiseaseFilter(selectedItem?.code || null)
              }
            />
          </Column>
          <Column sm={4} md={2} lg={4}>
            <TextInput
              id="search-text"
              labelText={intl.formatMessage({
                id: "label.search",
                defaultMessage: "Search",
              })}
              placeholder={intl.formatMessage({
                id: "label.search",
                defaultMessage: "Search...",
              })}
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
            />
          </Column>
          <Column sm={4} md={2} lg={4}>
            <div className="surveillance-filter-actions">
              <Button kind="primary" onClick={openCreateModal}>
                <FormattedMessage
                  id="surveillance.create.new"
                  defaultMessage="Create New"
                />
              </Button>
            </div>
          </Column>
        </Grid>

        {/* Table */}
        <DataTable rows={paginatedLocalizations} headers={headers}>
          {({
            rows,
            headers,
            getTableProps,
            getHeaderProps,
            getRowProps,
            onInputChange,
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
                  {tableLoading ? (
                    <TableRow>
                      <TableCell colSpan={7}>
                        <Loading withOverlay={false} small />
                      </TableCell>
                    </TableRow>
                  ) : paginatedLocalizations.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={7}>
                        <FormattedMessage
                          id="surveillance.no.data"
                          defaultMessage="No case definitions found."
                        />
                      </TableCell>
                    </TableRow>
                  ) : (
                    paginatedLocalizations.map((item) => (
                      <TableRow key={item.id}>
                        <TableCell>{item.id}</TableCell>
                        <TableCell>
                          {item.diseaseName || item.icdCode}
                        </TableCell>
                        <TableCell>{item.icdCode}</TableCell>
                        <TableCell>{item.version}</TableCell>
                        <TableCell>{item.description}</TableCell>
                        <TableCell>
                          <span
                            style={{
                              color: item.isActive ? "#198038" : "#da1e28",
                            }}
                          >
                            {item.isActive
                              ? intl.formatMessage({
                                  id: "label.active",
                                  defaultMessage: "Active",
                                })
                              : intl.formatMessage({
                                  id: "label.inactive",
                                  defaultMessage: "Inactive",
                                })}
                          </span>
                        </TableCell>
                        <TableCell>
                          <Button
                            kind="ghost"
                            size="sm"
                            hasIconOnly
                            renderIcon={Edit}
                            iconDescription={intl.formatMessage({
                              id: "label.edit",
                              defaultMessage: "Edit",
                            })}
                            onClick={() => handleEdit(item)}
                          />
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </DataTable>

        <Pagination
          totalItems={filteredLocalizations.length}
          pageSize={pageSize}
          pageSizes={[5, 10, 25, 50, 100]}
          page={currentPage}
          onChange={({ page, pageSize: newPageSize }) => {
            setCurrentPage(page);
            setPageSize(newPageSize);
          }}
        />
      </div>

      <Modal
        open={isCreateModalOpen}
        modalHeading={intl.formatMessage({
          id: editingItem
            ? "surveillance.edit.modalTitle"
            : "surveillance.create.modalTitle",
          defaultMessage: editingItem
            ? "Edit Case Definition"
            : "New Case Definition",
        })}
        primaryButtonText={intl.formatMessage({
          id: "label.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={closeCreateModal}
        onRequestSubmit={editingItem ? handleSave : handleCreateSubmit}
        size="lg"
      >
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "1fr 1fr",
            gap: "1rem",
            marginBottom: "1rem",
          }}
        >
          <ComboBox
            id="icd-code"
            titleText={intl.formatMessage({
              id: "label.icd.code",
              defaultMessage: "ICD Code",
            })}
            placeholder={intl.formatMessage({
              id: "label.icd.code",
              defaultMessage: "Select ICD Code",
            })}
            selectedItem={
              filteredIcdCodeOptions.find(
                (o) => o.code === createForm.icdCode,
              ) || null
            }
            items={filteredIcdCodeOptions}
            itemToString={(item) => (item ? item.label : "")}
            onInputChange={handleIcdCodeQuery}
            onChange={({ selectedItem }) =>
              handleCreateChange(
                "icdCode",
                selectedItem ? selectedItem.code : "",
              )
            }
          />
          <TextInput
            id="def-version"
            labelText={intl.formatMessage({
              id: "label.version",
              defaultMessage: "Version",
            })}
            type="number"
            value={createForm.version}
            onChange={(e) =>
              handleCreateChange("version", parseInt(e.target.value) || 1)
            }
          />
          <DatePicker
            datePickerType="single"
            value={createForm.effectiveDate}
            onChange={(dates) => {
              const d = dates[0];
              if (d) {
                const iso = d.toISOString().split("T")[0];
                handleCreateChange("effectiveDate", iso);
              }
            }}
          >
            <DatePickerInput
              id="effective-date"
              labelText={intl.formatMessage({
                id: "label.effective.date",
                defaultMessage: "Effective Date",
              })}
              placeholder="YYYY-MM-DD"
            />
          </DatePicker>
          <Select
            id="def-status"
            labelText={intl.formatMessage({
              id: "label.status",
              defaultMessage: "Status",
            })}
            value={createForm.isActive ? "active" : "inactive"}
            onChange={(e) =>
              handleCreateChange("isActive", e.target.value === "active")
            }
          >
            <SelectItem
              value="active"
              text={intl.formatMessage({
                id: "label.active",
                defaultMessage: "Active",
              })}
            />
            <SelectItem
              value="inactive"
              text={intl.formatMessage({
                id: "label.inactive",
                defaultMessage: "Inactive",
              })}
            />
          </Select>
        </div>
        <TextArea
          id="def-description"
          labelText={intl.formatMessage({
            id: "label.description",
            defaultMessage: "Description",
          })}
          value={createForm.description}
          onChange={(e) => handleCreateChange("description", e.target.value)}
          rows={3}
        />

        <div style={{ marginTop: "1.5rem" }}>
          <Accordion>
            <AccordionItem
              title={intl.formatMessage({
                id: "surveillance.threshold.title",
                defaultMessage: "Threshold & Notification",
              })}
              open
            >
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr 1fr",
                  gap: "0.75rem",
                  marginBottom: "1rem",
                }}
              >
                <Select
                  id="def-time-range"
                  labelText={intl.formatMessage({
                    id: "label.range.time",
                    defaultMessage: "Rentang Waktu",
                  })}
                  value={createForm.timeRange}
                  onChange={(e) =>
                    handleCreateChange("timeRange", e.target.value)
                  }
                >
                  {TIME_RANGE_OPTIONS.map((opt) => (
                    <SelectItem
                      key={opt.id || "_immediate"}
                      value={opt.id}
                      text={opt.label}
                    />
                  ))}
                </Select>
                <Select
                  id="def-threshold-type"
                  labelText={intl.formatMessage({
                    id: "label.threshold.type",
                    defaultMessage: "Jenis Ambang Batas",
                  })}
                  value={createForm.thresholdType}
                  onChange={(e) =>
                    handleCreateChange("thresholdType", e.target.value)
                  }
                >
                  {THRESHOLD_TYPE_OPTIONS.map((opt) => (
                    <SelectItem
                      key={opt.id || "_none"}
                      value={opt.id}
                      text={opt.label}
                    />
                  ))}
                </Select>
                <TextInput
                  id="def-threshold-value"
                  type="number"
                  labelText={intl.formatMessage({
                    id: "label.threshold.value",
                    defaultMessage: "Nilai Ambang Batas",
                  })}
                  value={createForm.thresholdValue}
                  onChange={(e) =>
                    handleCreateChange("thresholdValue", e.target.value)
                  }
                  disabled={
                    !createForm.thresholdType ||
                    createForm.thresholdType === "single_case"
                  }
                />
              </div>
              <div style={{ marginTop: "0.5rem" }}>
                <FilterableMultiSelect
                  id="def-recipients"
                  titleText={intl.formatMessage({
                    id: "surveillance.recipients.title",
                    defaultMessage: "Notification Recipients",
                  })}
                  placeholder={intl.formatMessage({
                    id: "surveillance.recipients.placeholder",
                    defaultMessage: "Select recipients",
                  })}
                  items={personOptions}
                  itemToString={(item) => item?.label || ""}
                  initialSelectedItems={personOptions.filter((p) =>
                    (createForm.recipients || []).some(
                      (r) => r.personId === p.id,
                    ),
                  )}
                  selectionFeedback="top-after-reopen"
                  onChange={({ selectedItems }) => {
                    const previous = createForm.recipients || [];
                    const updated = (selectedItems || []).map((p) => {
                      const existing = previous.find(
                        (r) => r.personId === p.id,
                      );
                      return {
                        personId: p.id,
                        notificationType: existing
                          ? existing.notificationType
                          : "all",
                      };
                    });
                    handleCreateChange("recipients", updated);
                  }}
                />
              </div>
              {(createForm.recipients || []).length > 0 && (
                <div style={{ marginTop: "0.75rem" }}>
                  {createForm.recipients.map((rec, rIdx) => {
                    const person = personOptions.find(
                      (p) => p.id === rec.personId,
                    );
                    return (
                      <div
                        key={rec.personId}
                        style={{
                          display: "grid",
                          gridTemplateColumns: "2fr 1fr auto",
                          gap: "0.75rem",
                          marginBottom: "0.5rem",
                          alignItems: "end",
                        }}
                      >
                        <div>{person ? person.label : rec.personId}</div>
                        <Select
                          id={`recipient-type-${rec.personId}`}
                          labelText={intl.formatMessage({
                            id: "surveillance.recipients.type",
                            defaultMessage: "Notification Type",
                          })}
                          value={rec.notificationType}
                          onChange={(e) => {
                            const updated = [...createForm.recipients];
                            updated[rIdx] = {
                              ...updated[rIdx],
                              notificationType: e.target.value,
                            };
                            handleCreateChange("recipients", updated);
                          }}
                        >
                          {NOTIFICATION_TYPE_OPTIONS.map((opt) => (
                            <SelectItem
                              key={opt.id}
                              value={opt.id}
                              text={opt.label}
                            />
                          ))}
                        </Select>
                        <Button
                          kind="danger--ghost"
                          size="sm"
                          onClick={() => {
                            const updated = createForm.recipients.filter(
                              (_, i) => i !== rIdx,
                            );
                            handleCreateChange("recipients", updated);
                          }}
                        >
                          <FormattedMessage
                            id="surveillance.recipients.remove"
                            defaultMessage="Remove"
                          />
                        </Button>
                      </div>
                    );
                  })}
                </div>
              )}
            </AccordionItem>
            <AccordionItem
              title={intl.formatMessage({
                id: "surveillance.rules.title",
                defaultMessage: "Case Conditions / Rules",
              })}
              open
            >
              {rulesLoading ? (
                <Loading withOverlay={false} small />
              ) : (
                <>
                  {(createForm.rules || []).map((rule, idx) => (
                    <div
                      key={idx}
                      style={{
                        border: "1px solid #e0e0e0",
                        borderRadius: "4px",
                        padding: "1rem",
                        marginBottom: "0.75rem",
                        background: "#f4f4f4",
                      }}
                    >
                      <div
                        style={{
                          display: "grid",
                          gridTemplateColumns: "1fr 1fr 1fr",
                          gap: "0.75rem",
                          marginBottom: "0.5rem",
                        }}
                      >
                        <TextInput
                          id={`rule-group-${idx}`}
                          labelText={intl.formatMessage({
                            id: "surveillance.rule.group",
                            defaultMessage: "Rule Group",
                          })}
                          type="number"
                          value={rule.ruleGroup}
                          onChange={(e) =>
                            handleRuleChange(
                              idx,
                              "ruleGroup",
                              parseInt(e.target.value) || 1,
                            )
                          }
                        />
                        <Select
                          id={`rule-operator-${idx}`}
                          labelText={intl.formatMessage({
                            id: "surveillance.rule.operator",
                            defaultMessage: isFirstInGroup(idx, rule.ruleGroup)
                              ? "Operator (first condition: none)"
                              : "Operator (connects to previous)",
                          })}
                          value={rule.operator}
                          onChange={(e) =>
                            handleRuleChange(idx, "operator", e.target.value)
                          }
                          disabled={isFirstInGroup(idx, rule.ruleGroup)}
                        >
                          <SelectItem value="" text="None (first condition)" />
                          <SelectItem value="AND" text="AND" />
                          <SelectItem value="OR" text="OR" />
                        </Select>
                        <ComboBox
                          id={`rule-testname-${idx}`}
                          titleText={intl.formatMessage({
                            id: "surveillance.rule.testName",
                            defaultMessage: "Test Name",
                          })}
                          placeholder={intl.formatMessage({
                            id: "surveillance.rule.testName.placeholder",
                            defaultMessage: "Select test",
                          })}
                          items={testOptions}
                          itemToString={(item) => item?.label || ""}
                          selectedItem={
                            testOptions.find((o) => o.id === rule.testName) ||
                            null
                          }
                          onChange={({ selectedItem }) =>
                            handleRuleChange(
                              idx,
                              "testName",
                              selectedItem ? selectedItem.id : "",
                            )
                          }
                        />
                      </div>
                      <div
                        style={{
                          display: "grid",
                          gridTemplateColumns: "1fr 1fr 1fr 1fr",
                          gap: "0.75rem",
                        }}
                      >
                        <Select
                          id={`rule-resulttype-${idx}`}
                          labelText={intl.formatMessage({
                            id: "surveillance.rule.resultType",
                            defaultMessage: "Result Type",
                          })}
                          value={rule.resultType}
                          onChange={(e) =>
                            handleRuleChange(idx, "resultType", e.target.value)
                          }
                        >
                          {RESULT_TYPE_OPTIONS.map((opt) => (
                            <SelectItem
                              key={opt.id}
                              value={opt.id}
                              text={opt.label}
                            />
                          ))}
                        </Select>
                        {rule.resultType === "D" && (
                          <TextInput
                            id={`rule-resultvalue-${idx}`}
                            labelText={intl.formatMessage({
                              id: "surveillance.rule.resultValue",
                              defaultMessage: "Result Value",
                            })}
                            value={rule.resultValue}
                            onChange={(e) =>
                              handleRuleChange(
                                idx,
                                "resultValue",
                                e.target.value,
                              )
                            }
                          />
                        )}
                        {rule.resultType === "N" && (
                          <>
                            <Select
                              id={`rule-numop-${idx}`}
                              labelText={intl.formatMessage({
                                id: "surveillance.rule.numericOperator",
                                defaultMessage: "Numeric Operator",
                              })}
                              value={rule.numericOperator}
                              onChange={(e) =>
                                handleRuleChange(
                                  idx,
                                  "numericOperator",
                                  e.target.value,
                                )
                              }
                            >
                              <SelectItem value="" text="Select" />
                              {NUMERIC_OPERATOR_OPTIONS.map((opt) => (
                                <SelectItem
                                  key={opt.id}
                                  value={opt.id}
                                  text={opt.label}
                                />
                              ))}
                            </Select>
                            <TextInput
                              id={`rule-numval-${idx}`}
                              labelText={intl.formatMessage({
                                id: "surveillance.rule.numericValue",
                                defaultMessage: "Numeric Value",
                              })}
                              type="number"
                              value={rule.numericValue}
                              onChange={(e) =>
                                handleRuleChange(
                                  idx,
                                  "numericValue",
                                  e.target.value,
                                )
                              }
                            />
                          </>
                        )}
                        <Button
                          kind="danger--ghost"
                          size="sm"
                          onClick={() => removeRule(idx)}
                          style={{ marginTop: "1.5rem" }}
                        >
                          <FormattedMessage
                            id="surveillance.rule.remove"
                            defaultMessage="Remove"
                          />
                        </Button>
                      </div>
                    </div>
                  ))}
                  <Button kind="tertiary" size="sm" onClick={addRule}>
                    <FormattedMessage
                      id="surveillance.rule.add"
                      defaultMessage="+ Add Rule"
                    />
                  </Button>
                </>
              )}
            </AccordionItem>
          </Accordion>
        </div>
      </Modal>
    </>
  );
};

export default SurveillanceConfiguration;
